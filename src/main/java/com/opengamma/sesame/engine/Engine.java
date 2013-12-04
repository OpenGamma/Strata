/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

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
import com.opengamma.sesame.ValuationTimeProvider;
import com.opengamma.sesame.ValuationTimeProviderFunction;
import com.opengamma.sesame.config.CompositeFunctionConfig;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.FunctionRepo;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphBuilder;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.marketdata.MarketDataProviderFunction;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.sesame.trace.FullTracer;
import com.opengamma.sesame.trace.NoOpTracer;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pairs;

/**
 * TODO this is totally provisional, just enough to run some basic tests that stitch everything together
 */
public class Engine {

  private static final Logger s_logger = LoggerFactory.getLogger(Engine.class);

  private final ExecutorService _executor;
  private final ComponentMap _components;
  private final FunctionRepo _functionRepo;
  private final FunctionConfig _defaultConfig;
  private final NodeDecorator _nodeDecorator;

  /* package */ Engine(ExecutorService executor, FunctionRepo functionRepo) {
    this(executor, ComponentMap.EMPTY, functionRepo, FunctionConfig.EMPTY, NodeDecorator.IDENTITY);
  }

  // TODO should any of these be arguments to createView()
  public Engine(ExecutorService executor,
                ComponentMap components,
                FunctionRepo functionRepo,
                FunctionConfig defaultConfig,
                NodeDecorator nodeDecorator) {
    _functionRepo = ArgumentChecker.notNull(functionRepo, "functionRepo");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _nodeDecorator = ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
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
    // TODO is this the right place for this logic? should there be a component map pre-populated with them?
    // as they're completely standard components always provided by the engine
    // need to supplement components with a MarketDataProviderFunction and ValuationTimeProviderFunction that are
    // under our control so we can switch out the backing impls each cycle if necessary
    DelegatingMarketDataProviderFunction marketDataProvider = new DelegatingMarketDataProviderFunction();
    ValuationTimeProvider valuationTimeProvider = new ValuationTimeProvider();
    Map<Class<?>, Object> componentOverrides = Maps.newHashMap();
    componentOverrides.put(MarketDataProviderFunction.class, marketDataProvider);
    componentOverrides.put(ValuationTimeProviderFunction.class, valuationTimeProvider);
    ComponentMap components = _components.with(componentOverrides);

    s_logger.debug("building graph model");
    GraphBuilder graphBuilder = new GraphBuilder(_functionRepo, components, _defaultConfig, _nodeDecorator);
    GraphModel graphModel = graphBuilder.build(viewDef, inputs);
    s_logger.debug("graph model complete, building graph");
    Graph graph = graphModel.build(components);
    s_logger.debug("graph complete");
    return new View(viewDef, graph, inputs, _executor, marketDataProvider, valuationTimeProvider, components, _defaultConfig);
  }

  //----------------------------------------------------------
  public static class View {

    private final Graph _graph;
    private final ViewDef _viewDef;
    private final List<?> _inputs;
    private final ExecutorService _executor;
    private final DelegatingMarketDataProviderFunction _marketDataProvider;
    private final ValuationTimeProvider _valuationTimeProvider;
    private final ComponentMap _components;
    private final FunctionConfig _systemDefaultConfig;

    private View(ViewDef viewDef,
                 Graph graph,
                 List<?> inputs,
                 ExecutorService executor,
                 DelegatingMarketDataProviderFunction marketDataProvider,
                 ValuationTimeProvider valuationTimeProvider,
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
