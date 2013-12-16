/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.change.ChangeEvent;
import com.opengamma.core.change.ChangeListener;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.change.ChangeType;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.DefaultValuationTimeFn;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.source.CacheAwareConfigSource;
import com.opengamma.sesame.cache.source.CacheAwareConventionSource;
import com.opengamma.sesame.cache.source.CacheAwareHistoricalTimeSeriesSource;
import com.opengamma.sesame.cache.source.CacheAwareRegionSource;
import com.opengamma.sesame.cache.source.CacheAwareSecuritySource;
import com.opengamma.sesame.config.CompositeFunctionConfig;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.sesame.trace.FullTracer;
import com.opengamma.sesame.trace.NoOpTracer;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
*
*/
public class View implements AutoCloseable {

  private static final Logger s_logger = LoggerFactory.getLogger(View.class);

  private final Graph _graph;
  private final ViewDef _viewDef;
  private final List<?> _inputs;
  private final ExecutorService _executor;
  private final DelegatingMarketDataFn _marketDataFn;
  private final DefaultValuationTimeFn _valuationTimeFn;
  private final ComponentMap _components;
  private final FunctionConfig _systemDefaultConfig;
  private final CompositeNodeDecorator _decorator;
  private final CacheInvalidator _cacheInvalidator;
  private final SourceListener _sourceListener = new SourceListener();
  private final Collection<ChangeManager> _changeManagers;

  // TODO this has too many parameters. does that matter? it's only called by the engine
  /* package */ View(ViewDef viewDef,
                     Graph graph,
                     List<?> inputs,
                     ExecutorService executor,
                     DelegatingMarketDataFn marketDataFn,
                     DefaultValuationTimeFn valuationTimeFn,
                     ComponentMap components,
                     FunctionConfig systemDefaultConfig,
                     CompositeNodeDecorator decorator,
                     CacheInvalidator cacheInvalidator) {
    _viewDef = ArgumentChecker.notNull(viewDef, "viewDef");
    _inputs = ArgumentChecker.notNull(inputs, "inputs");
    _graph = ArgumentChecker.notNull(graph, "graph");
    _executor = ArgumentChecker.notNull(executor, "executor");
    _marketDataFn = ArgumentChecker.notNull(marketDataFn, "marketDataFn");
    _valuationTimeFn = ArgumentChecker.notNull(valuationTimeFn, "valuationTimeFn");
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
    _systemDefaultConfig = ArgumentChecker.notNull(systemDefaultConfig, "systemDefaultConfig");
    _decorator = ArgumentChecker.notNull(decorator, "decorator");
    Pair<ComponentMap, Collection<ChangeManager>> pair =
        decorateSources(ArgumentChecker.notNull(components, "components"), _cacheInvalidator, _sourceListener);
    _components = pair.getFirst();
    _changeManagers = pair.getSecond();
  }

  /**
   * Runs a single calculation cycle, blocking until the results are available.
   * @param cycleArguments Settings for running the cycle including valuation time and market data source
   * @return The calculation results, not null
   */
  public synchronized Results run(CycleArguments cycleArguments) {
    initializeCycle(cycleArguments);
    List<Task> tasks = Lists.newArrayList();
    int colIndex = 0;
    List<ViewColumn> columns = _viewDef.getColumns();
    List<String> columnNames = Lists.newArrayListWithCapacity(columns.size());
    for (ViewColumn column : columns) {
      String columnName = column.getName();
      columnNames.add(columnName);
      Map<Class<?>, InvokableFunction> functions = _graph.getFunctionsForColumn(columnName);
      int rowIndex = 0;
      for (Object input : _inputs) {
        InvokableFunction function;
        InvokableFunction inputFunction = functions.get(input.getClass());
        // the input to the function can be a security when the input is a position or trade
        Object functionInput;
        if (inputFunction != null) {
          function = inputFunction;
          functionInput = input;
        } else if (input instanceof PositionOrTrade) {
          Security security = ((PositionOrTrade) input).getSecurity();
          function = functions.get(security.getClass());
          functionInput = security;
        } else {
          // this shouldn't happen if the graph is built correctly
          throw new OpenGammaRuntimeException("No function found for column " + column + " and " + input);
        }
        FunctionConfig functionConfig = CompositeFunctionConfig.compose(column.getFunctionConfig(functionInput.getClass()),
                                                                        _viewDef.getDefaultConfig(),
                                                                        _systemDefaultConfig);
        FunctionArguments args = functionConfig.getFunctionArguments(function.getReceiver().getClass());
        Tracer tracer;
        if (cycleArguments.getTraceFunctions().contains(Pairs.of(rowIndex, colIndex))) {
          tracer = new FullTracer();
        } else {
          tracer = NoOpTracer.INSTANCE;
        }
        tasks.add(new Task(functionInput, args, rowIndex++, colIndex, function, tracer));
      }
      colIndex++;
    }
    List<Future<TaskResult>> futures;
    try {
      futures = _executor.invokeAll(tasks);
    } catch (InterruptedException e) {
      throw new OpenGammaRuntimeException("Interrupted", e);
    }
    ResultBuilder resultsBuilder = Results.builder(_inputs, columnNames);
    for (Future<TaskResult> future : futures) {
      try {
        // TODO this probably won't do as a long term solution, it will block indefinitely if a function blocks
        TaskResult result = future.get();
        resultsBuilder.add(result._rowIndex, result._columnIndex, result._result, result._callGraph);
      } catch (InterruptedException | ExecutionException e) {
        s_logger.warn("Failed to get result from task", e);
      }
    }
    return resultsBuilder.build();
  }

  /**
   * Does the housekeeping tasks before running a calculation cycle.
   * This includes invalidating items in the cache that need to be recalculated, setting valuation times, setting
   * up market data etc.
   * @param cycleArguments Arguments for running the cycle
   */
  private void initializeCycle(CycleArguments cycleArguments) {
    // TODO this will need to be a lot cleverer when we need to support dynamic rebinding for full reval
    _cacheInvalidator.invalidate(cycleArguments.getMarketDataFactory(),
                                 cycleArguments.getValuationTime(),
                                 cycleArguments.getConfigVersionCorrection(),
                                 Collections.<ExternalId>emptyList(),
                                 _sourceListener.getIds());
    _sourceListener.clear();
    _marketDataFn.setDelegate(cycleArguments.getMarketDataFactory().create(_components));
    _valuationTimeFn.setValuationTime(cycleArguments.getValuationTime());
  }

  @Override
  public void close() {
    _decorator.close();
    for (ChangeManager changeManager : _changeManagers) {
      changeManager.removeChangeListener(_sourceListener);
    }
  }

  /**
   * Decorates the sources with cache aware versions that invalidate cache entries when the data changes.
   * The returned component map contains the cache aware sources in place of the originals.
   * The returns collection contains the change managers for all decorated sources. This allows listeners to
   * be removed when the view closes and prevents a resource leak.
   * @param components Platform components used by functions
   * @param cacheInvalidator For registering dependencies between values in the cache and the values used to calculate them
   * @param sourceListener Listens for changes in items in the database
   * @return A map of components containing the decorated sources instead of the originals, and a collection of
   * change managers which had a listener added to them
   */
  private Pair<ComponentMap, Collection<ChangeManager>> decorateSources(ComponentMap components,
                                                                        CacheInvalidator cacheInvalidator,
                                                                        SourceListener sourceListener) {
    Map<Class<?>, Object> sources = Maps.newHashMap();
    // need to record which ChangeManagers we're listening to so we can remove the listeners and avoid leaks
    Collection<ChangeManager> changeManagers = Lists.newArrayList();

    ConfigSource configSource = components.getComponent(ConfigSource.class);
    if (configSource != null) {
      changeManagers.add(configSource.changeManager());
      sources.put(ConfigSource.class, new CacheAwareConfigSource(configSource, cacheInvalidator));
    }

    RegionSource regionSource = components.getComponent(RegionSource.class);
    if (regionSource != null) {
      changeManagers.add(regionSource.changeManager());
      sources.put(RegionSource.class, new CacheAwareRegionSource(regionSource, cacheInvalidator));
    }

    SecuritySource securitySource = components.getComponent(SecuritySource.class);
    if (securitySource != null) {
      changeManagers.add(securitySource.changeManager());
      sources.put(SecuritySource.class, new CacheAwareSecuritySource(securitySource, cacheInvalidator));
    }

    ConventionSource conventionSource = components.getComponent(ConventionSource.class);
    if (conventionSource != null) {
      changeManagers.add(conventionSource.changeManager());
      sources.put(ConventionSource.class, new CacheAwareConventionSource(conventionSource, cacheInvalidator));
    }

    HistoricalTimeSeriesSource timeSeriesSource = components.getComponent(HistoricalTimeSeriesSource.class);
    if (timeSeriesSource != null) {
      changeManagers.add(timeSeriesSource.changeManager());
      sources.put(HistoricalTimeSeriesSource.class, new CacheAwareHistoricalTimeSeriesSource(timeSeriesSource, cacheInvalidator));
    }

    // TODO HolidaySource (which has a horrible design WRT decorating)

    for (ChangeManager changeManager : changeManagers) {
      changeManager.addChangeListener(sourceListener);
    }
    return Pairs.of(components, changeManagers);
  }

  // TODO run() variants that take:
  //   1) cycle and listener (for multiple execution)
  //   2) listener (for single async or infinite execution)
  //   3) new list of inputs

  //----------------------------------------------------------
  private static class TaskResult {

    private final int _rowIndex;
    private final int _columnIndex;
    private final Object _result;
    private final CallGraph _callGraph;

    private TaskResult(int rowIndex, int columnIndex, Object result, CallGraph callGraph) {
      _rowIndex = rowIndex;
      _columnIndex = columnIndex;
      _result = result;
      _callGraph = callGraph;
    }
  }

  //----------------------------------------------------------
  private static class Task implements Callable<TaskResult> {

    private final Object _input;
    private final int _rowIndex;
    private final int _columnIndex;
    private final InvokableFunction _invokableFunction;
    private final Tracer _tracer;
    private final FunctionArguments _args;

    private Task(Object input,
                 FunctionArguments args,
                 int rowIndex,
                 int columnIndex,
                 InvokableFunction invokableFunction,
                 Tracer tracer) {
      _input = input;
      _args = args;
      _rowIndex = rowIndex;
      _columnIndex = columnIndex;
      _invokableFunction = invokableFunction;
      _tracer = tracer;
    }

    @Override
    public TaskResult call() throws Exception {
      Object result;
      TracingProxy.start(_tracer);
      try {
        result = _invokableFunction.invoke(_input, _args);
      } catch (Exception e) {
        s_logger.warn("Failed to execute function", e);
        result = e;
      }
      CallGraph callGraph = TracingProxy.end();
      return new TaskResult(_rowIndex, _columnIndex, result, callGraph);
    }
  }

  /**
   * Listens to sources and records the IDs of any objects that are updated or removed.
   * This information is used to invalidate cache entries between cycles. Cached values are discarded if they were
   * calculated using objects that have since been updated.
   */
  private static class SourceListener implements ChangeListener {

    private final Queue<ObjectId> _ids = new ConcurrentLinkedQueue<>();

    @Override
    public void entityChanged(ChangeEvent event) {
      if (event.getType() == ChangeType.ADDED) {
        return;
      }
      _ids.add(event.getObjectId());
    }

    private Collection<ObjectId> getIds() {
      return _ids;
    }

    private void clear() {
      _ids.clear();
    }
  }
}
