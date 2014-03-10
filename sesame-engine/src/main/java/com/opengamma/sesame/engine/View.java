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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.config.CompositeFunctionModelConfig;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.NonPortfolioOutput;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;

/**
 *
 */
public class View implements AutoCloseable {

  private static final Logger s_logger = LoggerFactory.getLogger(View.class);

  private final Graph _graph;
  private final ViewConfig _viewConfig;
  private final List<?> _inputs;
  private final ExecutorService _executor;
  private final FunctionModelConfig _systemDefaultConfig;
  private final NodeDecorator _decorator;
  private final CacheInvalidator _cacheInvalidator;
  private final ViewFactory.SourceListener _sourceListener;
  private final Collection<ChangeManager> _changeManagers;
  private final List<String> _columnNames;
  private final GraphModel _graphModel;

  // TODO this has too many parameters. does that matter? it's only called by the engine
  /* package */ View(ViewConfig viewConfig,
                     Graph graph,
                     List<?> inputs,
                     ExecutorService executor,
                     FunctionModelConfig systemDefaultConfig,
                     NodeDecorator decorator,
                     CacheInvalidator cacheInvalidator,
                     GraphModel graphModel,
                     ViewFactory.SourceListener sourceListener,
                     Collection<ChangeManager> changeManagers) {
    _sourceListener = ArgumentChecker.notNull(sourceListener, "sourceListener");
    _graphModel = ArgumentChecker.notNull(graphModel, "graphModel");
    _viewConfig = ArgumentChecker.notNull(viewConfig, "viewConfig");
    _inputs = ArgumentChecker.notNull(inputs, "inputs");
    _graph = ArgumentChecker.notNull(graph, "graph");
    _executor = ArgumentChecker.notNull(executor, "executor");
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
    _systemDefaultConfig = ArgumentChecker.notNull(systemDefaultConfig, "systemDefaultConfig");
    _decorator = ArgumentChecker.notNull(decorator, "decorator");
    _changeManagers = ArgumentChecker.notNull(changeManagers, "changeManagers");
    _columnNames = columnNames(_viewConfig);
  }

  /**
   * Runs a single calculation cycle, blocking until the results are available.
   * @param cycleArguments Settings for running the cycle including valuation time and market data source
   * @return The calculation results, not null
   */
  public synchronized Results run(CycleArguments cycleArguments) {
    EngineEnvironment env = new EngineEnvironment(cycleArguments.getValuationTime(),
                                                  cycleArguments.getMarketDataSource(),
                                                  _cacheInvalidator);
    invalidateCache(cycleArguments);
    List<Task> tasks = Lists.newArrayList();
    tasks.addAll(portfolioTasks(env, cycleArguments));
    tasks.addAll(nonPortfolioTasks(env, cycleArguments));
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

  private List<Task> portfolioTasks(Environment env, CycleArguments cycleArguments) {
    // create tasks for the portfolio outputs
    int colIndex = 0;
    List<Task> portfolioTasks = Lists.newArrayList();
    for (ViewColumn column : _viewConfig.getColumns()) {
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
        Tracer tracer = Tracer.create(cycleArguments.isTracingEnabled(rowIndex, colIndex));

        FunctionModelConfig functionModelConfig = CompositeFunctionModelConfig.compose(
            column.getFunctionConfig(functionInput.getClass()),
            _viewConfig.getDefaultConfig(),
            _systemDefaultConfig);

        FunctionArguments args = functionModelConfig.getFunctionArguments(function.getUnderlyingReceiver().getClass());
        portfolioTasks.add(new PortfolioTask(env, functionInput, args, rowIndex++, colIndex, function, tracer));
      }
      colIndex++;
    }
    return portfolioTasks;
  }

  // create tasks for the non-portfolio outputs
  private List<Task> nonPortfolioTasks(Environment env, CycleArguments cycleArguments) {
    List<Task> tasks = Lists.newArrayList();
    for (NonPortfolioOutput output : _viewConfig.getNonPortfolioOutputs()) {
      InvokableFunction function = _graph.getNonPortfolioFunction(output.getName());
      Tracer tracer = Tracer.create(cycleArguments.isTracingEnabled(output.getName()));

      FunctionModelConfig functionModelConfig = CompositeFunctionModelConfig.compose(
          output.getOutput().getFunctionModelConfig(),
          _viewConfig.getDefaultConfig(),
          _systemDefaultConfig);

      FunctionArguments args = functionModelConfig.getFunctionArguments(function.getUnderlyingReceiver().getClass());
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

  /**
   * Does the housekeeping tasks before running a calculation cycle.
   * This includes removing items from the cache that need to be recalculated, setting valuation times, setting
   * up market data etc.
   * @param cycleArguments Arguments for running the cycle
   */
  private void invalidateCache(CycleArguments cycleArguments) {
    // TODO need to query the market data factory to see what data has changed during the cycle
    //   for live sources this will be individual values
    //   for snapshots it will be the entire snapshot if it's been updated in the DB
    //   if the data provider has completely changed then everything must go (which is currently done in the invalidator)
    // TODO this needs to be integrated with ServiceContext
    _cacheInvalidator.invalidate(cycleArguments.getMarketDataSource(),
                                 cycleArguments.getValuationTime(),
                                 cycleArguments.getConfigVersionCorrection(),
                                 Collections.<ExternalId>emptyList(),
                                 _sourceListener.getIds());
    _sourceListener.clear();
  }

  @Override
  public void close() {
    _decorator.close();
    for (ChangeManager changeManager : _changeManagers) {
      changeManager.removeChangeListener(_sourceListener);
    }
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
        return retVal instanceof Result ? (Result<?>) retVal : ResultGenerator.success(retVal);
      } catch (Exception e) {
        s_logger.warn("Failed to execute function", e);
        return ResultGenerator.failure(e);
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
