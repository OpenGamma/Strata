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
import com.opengamma.sesame.config.CompositeFunctionModelConfig;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.NonPortfolioOutput;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
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
  private final FunctionModelConfig _systemDefaultConfig;
  private final CompositeNodeDecorator _decorator;
  private final CacheInvalidator _cacheInvalidator;
  private final SourceListener _sourceListener = new SourceListener();
  private final Collection<ChangeManager> _changeManagers;
  private final List<String> _columnNames;

  // TODO this has too many parameters. does that matter? it's only called by the engine
  /* package */ View(ViewDef viewDef,
                     Graph graph,
                     List<?> inputs,
                     ExecutorService executor,
                     DelegatingMarketDataFn marketDataFn,
                     DefaultValuationTimeFn valuationTimeFn,
                     ComponentMap components,
                     FunctionModelConfig systemDefaultConfig,
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
    _columnNames = columnNames(_viewDef);
  }

  /**
   * Runs a single calculation cycle, blocking until the results are available.
   * @param cycleArguments Settings for running the cycle including valuation time and market data source
   * @return The calculation results, not null
   */
  public synchronized Results run(CycleArguments cycleArguments) {
    initializeCycle(cycleArguments);
    List<Task> tasks = Lists.newArrayList();
    tasks.addAll(portfolioTasks(cycleArguments));
    tasks.addAll(nonPortfolioTasks(cycleArguments));
    List<Future<TaskResult>> futures;
    try {
      futures = _executor.invokeAll(tasks);
    } catch (InterruptedException e) {
      throw new OpenGammaRuntimeException("Interrupted", e);
    }
    ResultBuilder resultsBuilder = Results.builder(_inputs, _columnNames);
    for (Future<TaskResult> future : futures) {
      try {
        TaskResult result = future.get();
        result.addToResults(resultsBuilder);
      } catch (InterruptedException | ExecutionException e) {
        s_logger.warn("Failed to get result from task", e);
      }
    }
    return resultsBuilder.build();
  }

  private List<Task> portfolioTasks(CycleArguments cycleArguments) {
    // create tasks for the portfolio outputs
    int colIndex = 0;
    List<Task> portfolioTasks = Lists.newArrayList();
    for (ViewColumn column : _viewDef.getColumns()) {
      Map<Class<?>, InvokableFunction> functions = _graph.getFunctionsForColumn(column.getName());
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
        FunctionModelConfig functionModelConfig = CompositeFunctionModelConfig.compose(column.getFunctionConfig(functionInput.getClass()),
                                                                                       _viewDef.getDefaultConfig(),
                                                                                       _systemDefaultConfig);
        FunctionArguments args = functionModelConfig.getFunctionArguments(function.getReceiver().getClass());
        Tracer tracer = Tracer.create(cycleArguments.isTracingEnabled(rowIndex, colIndex));
        portfolioTasks.add(new PortfolioTask(functionInput, args, rowIndex++, colIndex, function, tracer));
      }
      colIndex++;
    }
    return portfolioTasks;
  }

  // create tasks for the non-portfolio outputs
  private List<Task> nonPortfolioTasks(CycleArguments cycleArguments) {
    List<Task> tasks = Lists.newArrayList();
    for (NonPortfolioOutput output : _viewDef.getNonPortfolioOutputs()) {
      InvokableFunction function = _graph.getNonPortfolioFunction(output.getName());
      Tracer tracer = Tracer.create(cycleArguments.isTracingEnabled(output.getName()));
      FunctionModelConfig functionModelConfig = output.getOutput().getFunctionModelConfig();
      FunctionArguments args = functionModelConfig.getFunctionArguments(function.getReceiver().getClass());
      tasks.add(new NonPortfolioTask(args, output.getName(), function, tracer));
    } return tasks;
  }

  private static List<String> columnNames(ViewDef viewDef) {
    List<String> columnNames = Lists.newArrayListWithCapacity(viewDef.getColumns().size());
    for (ViewColumn column : viewDef.getColumns()) {
      String columnName = column.getName();
      columnNames.add(columnName);
    }
    return columnNames;
  }

  /**
   * Does the housekeeping tasks before running a calculation cycle.
   * This includes removing items from the cache that need to be recalculated, setting valuation times, setting
   * up market data etc.
   * @param cycleArguments Arguments for running the cycle
   */
  private void initializeCycle(CycleArguments cycleArguments) {
    // TODO this will need to be a lot cleverer when we need to support dynamic rebinding for full reval
    // it's possible there will be multiple top-level contexts with their own valuation time, version correction,
    // market data, cache/invalidator etc. how do these interact with the cycle? or are they done at a higher
    // level than a view? i.e. multiple views running in parallel?
    // TODO need to query the market data factory to see what data has changed during the cycle
    //   for live sources this will be individual values
    //   for snapshots it will be the entire snapshot if it's been updated in the DB
    //   if the data provider has completely changed then everything must go (which is currently done in the invalidator)
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

    ConfigSource configSource = components.findComponent(ConfigSource.class);
    if (configSource != null) {
      changeManagers.add(configSource.changeManager());
      sources.put(ConfigSource.class, new CacheAwareConfigSource(configSource, cacheInvalidator));
    }

    RegionSource regionSource = components.findComponent(RegionSource.class);
    if (regionSource != null) {
      changeManagers.add(regionSource.changeManager());
      sources.put(RegionSource.class, new CacheAwareRegionSource(regionSource, cacheInvalidator));
    }

    SecuritySource securitySource = components.findComponent(SecuritySource.class);
    if (securitySource != null) {
      changeManagers.add(securitySource.changeManager());
      sources.put(SecuritySource.class, new CacheAwareSecuritySource(securitySource, cacheInvalidator));
    }

    ConventionSource conventionSource = components.findComponent(ConventionSource.class);
    if (conventionSource != null) {
      changeManagers.add(conventionSource.changeManager());
      sources.put(ConventionSource.class, new CacheAwareConventionSource(conventionSource, cacheInvalidator));
    }

    HistoricalTimeSeriesSource timeSeriesSource = components.findComponent(HistoricalTimeSeriesSource.class);
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
  private interface TaskResult {

    void addToResults(ResultBuilder resultBuilder);
  }

  //----------------------------------------------------------
  private abstract static class Task implements Callable<TaskResult> {

    private final Object _input;
    private final InvokableFunction _invokableFunction;
    private final Tracer _tracer;
    private final FunctionArguments _args;

    private Task(Object input,
                 FunctionArguments args,
                 InvokableFunction invokableFunction,
                 Tracer tracer) {
      _input = input;
      _args = args;
      _invokableFunction = invokableFunction;
      _tracer = tracer;
    }

    @Override
    public TaskResult call() throws Exception {
      Result<?> result;
      TracingProxy.start(_tracer);
      try {
        Object retVal = _invokableFunction.invoke(_input, _args);
        if (retVal instanceof Result) {
          result = (Result) retVal;
        } else {
          result = ResultGenerator.success(retVal);
        }
      } catch (Exception e) {
        s_logger.warn("Failed to execute function", e);
        // TODO ResultGenerator needs to handle exceptions properly. fix this when it does
        result = ResultGenerator.failure(FailureStatus.ERROR, e.getMessage());
      }
      CallGraph callGraph = TracingProxy.end();
      return createResult(result, callGraph);
    }

    protected abstract TaskResult createResult(Result<?> result, CallGraph callGraph);
  }

  //----------------------------------------------------------
  private static class PortfolioTask extends Task {

    private final int _rowIndex;
    private final int _columnIndex;

    private PortfolioTask(Object input,
                          FunctionArguments args,
                          int rowIndex,
                          int columnIndex,
                          InvokableFunction invokableFunction,
                          Tracer tracer) {
      super(input, args, invokableFunction, tracer);
      _rowIndex = rowIndex;
      _columnIndex = columnIndex;
    }

    @Override
    protected TaskResult createResult(final Result<?> result, final CallGraph callGraph) {
      return new TaskResult() {
        @Override
        public void addToResults(ResultBuilder resultBuilder) {
          resultBuilder.add(_rowIndex, _columnIndex, result, callGraph);
        }
      };
    }
  }

  //----------------------------------------------------------
  private static class NonPortfolioTask extends Task {

    private final String _outputValueName;

    private NonPortfolioTask(FunctionArguments args,
                             String outputValueName,
                             InvokableFunction invokableFunction,
                             Tracer tracer) {
      super(null, args, invokableFunction, tracer);
      _outputValueName = ArgumentChecker.notEmpty(outputValueName, "outputValueName");
    }

    @Override
    protected TaskResult createResult(final Result<?> result, final CallGraph callGraph) {
      return new TaskResult() {
        @Override
        public void addToResults(ResultBuilder resultBuilder) {
          resultBuilder.add(_outputValueName, result, callGraph);
        }
      };
    }
  }

  //----------------------------------------------------------
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
