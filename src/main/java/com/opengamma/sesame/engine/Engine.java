/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.DefaultValuationTimeFn;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.config.CompositeFunctionConfig;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphBuilder;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.sesame.trace.FullTracer;
import com.opengamma.sesame.trace.NoOpTracer;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pairs;

import net.sf.ehcache.CacheManager;

/**
 * TODO does the engine actually need to exist? all it does is store a couple of fields and create a view
 * could the views be created directly? or should it just be renamed ViewFactory?
 */
public class Engine {

  private static final Logger s_logger = LoggerFactory.getLogger(Engine.class);

  private final ExecutorService _executor;
  private final ComponentMap _components;
  private final AvailableOutputs _availableOutputs;
  private final AvailableImplementations _availableImplementations;
  private final EnumSet<EngineService> _defaultServices;
  private final CacheManager _cacheManager;
  private final FunctionConfig _defaultConfig;

  /* package */ Engine(ExecutorService executor,
                       AvailableOutputs availableOutputs,
                       AvailableImplementations availableImplementations) {
    this(executor,
         ComponentMap.EMPTY,
         availableOutputs,
         availableImplementations,
         FunctionConfig.EMPTY,
         CacheManager.getInstance(),
         EngineService.DEFAULT_SERVICES);
  }

  // TODO parameter to allow arbitrary NodeDecorators to be passed in?
  public Engine(ExecutorService executor,
                ComponentMap components,
                AvailableOutputs availableOutputs,
                AvailableImplementations availableImplementations,
                FunctionConfig defaultConfig,
                CacheManager cacheManager,
                EnumSet<EngineService> defaultServices) {
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "availableImplementations");
    _defaultServices = ArgumentChecker.notNull(defaultServices, "defaultServices");
    _cacheManager = ArgumentChecker.notNull(cacheManager, "cacheManager");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _executor = ArgumentChecker.notNull(executor, "executor");
    _components = ArgumentChecker.notNull(components, "components");
  }

  /*public interface Listener {

    void cycleComplete(Results results);
  }*/


  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}. This will be relaxed
   * in future.
   */
  public View createView(ViewDef viewDef, List<?> inputs) {
    return createView(viewDef, inputs, _defaultServices);
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}. This will be relaxed
   * in future.
   * TODO parameter to allow arbitrary NodeDecorators to be passed in?
   */
  public View createView(ViewDef viewDef, List<?> inputs, EnumSet<EngineService> services) {
    // TODO is this the right place for this logic? should there be a component map pre-populated with them?
    // as they're completely standard components always provided by the engine
    // need to supplement components with a MarketDataFn and ValuationTimeFn that are
    // under our control so we can switch out the backing impls each cycle if necessary
    DelegatingMarketDataFn marketDataFn = new DelegatingMarketDataFn();
    DefaultValuationTimeFn valuationTimeFn = new DefaultValuationTimeFn();
    Map<Class<?>, Object> componentOverrides = Maps.newHashMap();
    componentOverrides.put(MarketDataFn.class, marketDataFn);
    componentOverrides.put(ValuationTimeFn.class, valuationTimeFn);
    ComponentMap components = _components.with(componentOverrides);

    // TODO should the node decorator be built here? or passed in here? one cache per view?
    // whose job is it to build the decorators? probably should be in here for the standard set
    // should there be an enum set arg specifying which standard decorators to use? caching, timing, tracing

    s_logger.debug("building graph model");
    GraphBuilder graphBuilder = new GraphBuilder(_availableOutputs,
                                                 _availableImplementations,
                                                 components,
                                                 _defaultConfig,
                                                 createDecorator(services));
    GraphModel graphModel = graphBuilder.build(viewDef, inputs);
    s_logger.debug("graph model complete, building graph");
    Graph graph = graphModel.build(components);
    s_logger.debug("graph complete");
    return new View(viewDef, graph, inputs, _executor, marketDataFn, valuationTimeFn, components, _defaultConfig);
  }

  private NodeDecorator createDecorator(EnumSet<EngineService> services) {
    if (services.isEmpty()) {
      return NodeDecorator.IDENTITY;
    }
    List<NodeDecorator> decorators = Lists.newArrayListWithCapacity(services.size());
    // TODO wire up caching proxy to other cache-related stuff (e.g. invalidator)
    if (services.contains(EngineService.CACHING)) {
      decorators.add(new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal()));
    }
    if (services.contains(EngineService.TIMING)) {
      decorators.add(TimingProxy.INSTANCE);
    }
    if (services.contains(EngineService.TRACING)) {
      decorators.add(TracingProxy.INSTANCE);
    }
    return new CompositeNodeDecorator(decorators);
  }

  //----------------------------------------------------------
  public static class View {

    private final Graph _graph;
    private final ViewDef _viewDef;
    private final List<?> _inputs;
    private final ExecutorService _executor;
    private final DelegatingMarketDataFn _marketDataProvider;
    private final DefaultValuationTimeFn _valuationTimeProvider;
    private final ComponentMap _components;
    private final FunctionConfig _systemDefaultConfig;

    private View(ViewDef viewDef,
                 Graph graph,
                 List<?> inputs,
                 ExecutorService executor,
                 DelegatingMarketDataFn marketDataProvider,
                 DefaultValuationTimeFn valuationTimeProvider,
                 ComponentMap components,
                 FunctionConfig systemDefaultConfig) {
      _viewDef = viewDef;
      _inputs = inputs;
      _graph = graph;
      _executor = executor;
      _marketDataProvider = marketDataProvider;
      _valuationTimeProvider = valuationTimeProvider;
      _components = components;
      _systemDefaultConfig = systemDefaultConfig;
    }

    // TODO should this be synchronized?
    public Results run(CycleArguments cycleArguments) {
      // TODO this will need to be a lot cleverer when we need to support dynamic rebinding for full reval
      _marketDataProvider.setDelegate(cycleArguments.getMarketDataFactory().create(_components));
      _valuationTimeProvider.setValuationTime(cycleArguments.getValuationTime());
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
  }
}
