/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.CompositeFunctionArguments;
import com.opengamma.sesame.config.CompositeFunctionModelConfig;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.NonPortfolioOutput;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.function.PermissionDeniedFunction;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * <p>View is the main class for running calculations over a portfolio and producing results.</p>
 *
 * TODO scenarios - at what level should the arguments be supplied? there are several obvious options
 *   1) when the view is created, apply to all calculations
 *   2) when the view is created, different sets for different columns (maybe with a set that applies to all)
 *   3) when the view is run, apply to all calculations
 *   4) when the view is run, different sets for different columns (maybe with a set that applies to all)
 *   5) something else?
 * which ones do we need to support? all of them?
 * TODO same question for method arguments for the top level functions
 * see [SSM-182] and [SSM-185]
 */
public class View {

  private static final Logger s_logger = LoggerFactory.getLogger(View.class);

  private final Graph _graph;
  private final ViewConfig _viewConfig;
  private final ExecutorService _executor;
  private final FunctionModelConfig _systemDefaultConfig;
  private final CachingManager _cachingManager;
  private final List<String> _columnNames;
  private final GraphModel _graphModel;

  // TODO this has too many parameters. does that matter? it's only called by the view factory
  View(ViewConfig viewConfig,
       Graph graph,
       ExecutorService executor,
       FunctionModelConfig systemDefaultConfig,
       // TODO - passing in cachingManager is not ideal - should be removed later
       CachingManager cachingManager,
       GraphModel graphModel) {
    _graphModel = ArgumentChecker.notNull(graphModel, "graphModel");
    _viewConfig = ArgumentChecker.notNull(viewConfig, "viewConfig");
    _graph = ArgumentChecker.notNull(graph, "graph");
    _executor = ArgumentChecker.notNull(executor, "executor");
    _cachingManager = ArgumentChecker.notNull(cachingManager, "cachingManager");
    _systemDefaultConfig = ArgumentChecker.notNull(systemDefaultConfig, "systemDefaultConfig");
    _columnNames = columnNames(_viewConfig);
  }

  /**
   * Runs a single calculation cycle, blocking until the results are available.
   * @param cycleArguments Settings for running the cycle including valuation time and market data source
   * @return The calculation results, not null
   */
  public synchronized Results run(CycleArguments cycleArguments) {
    return run(cycleArguments, Collections.emptyList());
  }

  /**
   * Runs a single calculation cycle, blocking until the results are available.
   * @param cycleArguments Settings for running the cycle including valuation time and market data source
   * @param inputs the inputs to the calculation, e.g. trades, positions, securities
   * @return The calculation results, not null
   * TODO this should be re-entrant for non-live views that can be run in parallel (e.g. multiple scenarios)
   * would need a different (no-op?) cache invalidator. the view factory needs to know whether a view is live
   * (and therefore sequential) or can be run in parallel.
   */
  public synchronized Results run(CycleArguments cycleArguments, List<?> inputs) {

    ServiceContext originalContext = ThreadLocalServiceContext.getInstance();
    CycleInitializer cycleInitializer = cycleArguments.isCaptureInputs() ?
        new CapturingCycleInitializer(originalContext, _cachingManager, cycleArguments,
                                      _graphModel, _viewConfig, inputs) :
        new StandardCycleInitializer(originalContext, cycleArguments.getMarketDataSource(), _graph);

    Environment env = new EngineEnvironment(cycleArguments.getValuationTime(),
                                            cycleInitializer.getMarketDataSource(),
                                            cycleArguments.getScenarioArguments(),
                                            _cachingManager.getCacheInvalidator());

    List<Task> tasks = new ArrayList<>();
    Graph graph = cycleInitializer.getGraph();
    tasks.addAll(portfolioTasks(env, cycleArguments, inputs, graph));
    tasks.addAll(nonPortfolioTasks(env, cycleArguments, graph));
    List<Future<TaskResult>> futures;

    try {
      // Create a new version of the context with our wrapped components
      // Using the with method means we don't need to provide other
      // items e.g. VersionCorrectionProvider
      // TODO - ultimately we will want to set VersionCorrection here
      ThreadLocalServiceContext.init(cycleInitializer.getServiceContext());
      futures = _executor.invokeAll(tasks);
    } catch (InterruptedException e) {
      throw new OpenGammaRuntimeException("Interrupted", e);
    } finally {
      // Switch the service context back now all the work is done
      ThreadLocalServiceContext.init(originalContext);
    }

    ResultBuilder resultsBuilder = Results.builder(inputs, _columnNames);
    for (Future<TaskResult> future : futures) {
      try {
        TaskResult result = future.get();
        result.addToResults(resultsBuilder);
      } catch (InterruptedException | ExecutionException e) {
        s_logger.warn("Failed to get result from task", e);
      }
    }

    return cycleInitializer.complete(resultsBuilder.build());
  }

  /**
   * Returns the {@link FunctionModel} of the function used to calculate the value in a column.
   * @param columnName the name of the column
   * @param inputType type of input (i.e. the security, trade or position type) for the row
   * @return the function model or null if there isn't one for the specified input type
   * @throws IllegalArgumentException if the column name isn't found
   */
  public FunctionModel getFunctionModel(String columnName, Class<?> inputType) {
    return _graphModel.getFunctionModel(columnName, inputType);
  }

  /**
   * Returns the {@link FunctionModel} of the function used to calculate a non-portfolio output.
   * @param outputName The name of the output
   * @return the function model
   * @throws IllegalArgumentException if the output name isn't found
   */
  public FunctionModel getFunctionModel(String outputName) {
    return _graphModel.getFunctionModel(outputName);
  }

  private List<Task> portfolioTasks(Environment env, CycleArguments cycleArguments, List<?> inputs, Graph graph) {
    // create tasks for the portfolio outputs
    int colIndex = 0;
    List<Task> portfolioTasks = Lists.newArrayList();
    for (ViewColumn column : _viewConfig.getColumns()) {
      Map<Class<?>, InvokableFunction> functions = graph.getFunctionsForColumn(column.getName());
      int rowIndex = 0;
      for (Object input : inputs) {
        InvokableFunction function;
        InvokableFunction inputFunction = functions.get(input.getClass());
        // the input to the function can be a security when the input is a position or trade
        Object functionInput;
        if (inputFunction != null) {
          function = inputFunction;
          functionInput = input;
        } else if (input instanceof PositionOrTrade) {
          try {
            Security security = ((PositionOrTrade) input).getSecurity();
            function = functions.get(security.getClass());
            functionInput = security;
          } catch (AuthorizationException e) {
            function = new PermissionDeniedFunction(e.getMessage());
            functionInput = input;
          }
        } else {
          function = null;
          functionInput = null;
        }
        if (function == null) {
          // this shouldn't happen if the graph is built correctly
          throw new OpenGammaRuntimeException("No function found for column " + column + " and " + input);
        }
        Tracer tracer = Tracer.create(cycleArguments.isTracingEnabled(rowIndex, colIndex));

        FunctionModelConfig functionModelConfig = CompositeFunctionModelConfig.compose(
            column.getFunctionConfig(functionInput.getClass()),
            _viewConfig.getDefaultConfig(),
            _systemDefaultConfig);

        Class<?> implType = function.getUnderlyingReceiver().getClass();
        Class<?> declaringType = function.getDeclaringClass();
        FunctionArguments args = CompositeFunctionArguments.compose(cycleArguments.getFunctionArguments(),
                                                                    functionModelConfig.getFunctionArguments(implType),
                                                                    functionModelConfig.getFunctionArguments(declaringType));
        portfolioTasks.add(new PortfolioTask(env, functionInput, args, rowIndex++, colIndex, function, tracer));
      }
      colIndex++;
    }
    return portfolioTasks;
  }

  // create tasks for the non-portfolio outputs
  private List<Task> nonPortfolioTasks(Environment env, CycleArguments cycleArguments, Graph graph) {
    List<Task> tasks = Lists.newArrayList();
    for (NonPortfolioOutput output : _viewConfig.getNonPortfolioOutputs()) {
      InvokableFunction function = graph.getNonPortfolioFunction(output.getName());
      Tracer tracer = Tracer.create(cycleArguments.isTracingEnabled(output.getName()));

      FunctionModelConfig functionModelConfig = CompositeFunctionModelConfig.compose(
          output.getOutput().getFunctionModelConfig(),
          _viewConfig.getDefaultConfig(),
          _systemDefaultConfig);

      Class<?> implType = function.getUnderlyingReceiver().getClass();
      Class<?> declaringType = function.getDeclaringClass();
      FunctionArguments args = CompositeFunctionArguments.compose(cycleArguments.getFunctionArguments(),
                                                                  functionModelConfig.getFunctionArguments(implType),
                                                                  functionModelConfig.getFunctionArguments(declaringType));
      tasks.add(new NonPortfolioTask(env, args, output.getName(), function, tracer));
    } return tasks;
  }

  private static List<String> columnNames(ViewConfig viewConfig) {
    List<String> columnNames = Lists.newArrayListWithCapacity(viewConfig.getColumns().size());
    for (ViewColumn column : viewConfig.getColumns()) {
      String columnName = column.getName();
      columnNames.add(columnName);
    }
    return columnNames;
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

    private final Environment _env;
    private final Object _input;
    private final InvokableFunction _invokableFunction;
    private final Tracer _tracer;
    private final FunctionArguments _args;

    private Task(Environment env,
                 Object input,
                 FunctionArguments args,
                 InvokableFunction invokableFunction,
                 Tracer tracer) {
      _env = env;
      _input = input;
      _args = args;
      _invokableFunction = invokableFunction;
      _tracer = tracer;
    }

    @Override
    public TaskResult call() throws Exception {
      TracingProxy.start(_tracer);
      Result<?> result = invokeFunction();
      CallGraph callGraph = TracingProxy.end();
      return createResult(result, callGraph);
    }

    private Result<?> invokeFunction() {
      try {
        Object retVal = _invokableFunction.invoke(_env, _input, _args);
        return retVal instanceof Result ? (Result<?>) retVal : Result.success(retVal);
      } catch (Exception e) {
        s_logger.warn("Failed to execute function", e);
        return Result.failure(e);
      }
    }

    protected abstract TaskResult createResult(Result<?> result, CallGraph callGraph);
  }

  //----------------------------------------------------------
  private static final class PortfolioTask extends Task {

    private final int _rowIndex;
    private final int _columnIndex;

    private PortfolioTask(Environment env,
                          Object input,
                          FunctionArguments args,
                          int rowIndex,
                          int columnIndex,
                          InvokableFunction invokableFunction,
                          Tracer tracer) {
      super(env, input, args, invokableFunction, tracer);
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
  private static final class NonPortfolioTask extends Task {

    private final String _outputValueName;

    private NonPortfolioTask(Environment env,
                             FunctionArguments args,
                             String outputValueName,
                             InvokableFunction invokableFunction,
                             Tracer tracer) {
      super(env, null, args, invokableFunction, tracer);
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
}
