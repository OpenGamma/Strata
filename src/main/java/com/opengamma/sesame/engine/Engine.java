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
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueIdentifiable;
import com.opengamma.sesame.config.ColumnOutput;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.FunctionRepo;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.graph.FunctionGraph;
import com.opengamma.sesame.graph.GraphModel;

/**
 * TODO this is totally provisional, just enough to run some basic tests that stitch everything together
 */
/* package */ class Engine {

  private static final Logger s_logger = LoggerFactory.getLogger(Engine.class);

  private final ExecutorService _executor;
  private final Map<Class<?>, Object> _infrastructure;
  private final FunctionRepo _functionRepo;

  /* package */ Engine(ExecutorService executor, FunctionRepo functionRepo) {
    this(executor, Collections.<Class<?>, Object>emptyMap(), functionRepo);
  }
  /* package */ Engine(ExecutorService executor, Map<Class<?>, Object> infrastructure, FunctionRepo functionRepo) {
    _executor = executor;
    _infrastructure = infrastructure;
    _functionRepo = functionRepo;
  }

  public interface Listener {

    void cycleComplete(Results results);
  }

  // TODO allow targets to be anything? would allow support for parallelization, e.g. List<SwapSecurity>
  // might have to make target type an object instead of a type param on OutputFunction to cope with erasure
  public View createView(ViewDef viewDef, Collection<? extends PositionOrTrade> targets, Listener listener) {
    GraphModel graphModel = GraphModel.forView(viewDef, targets, _infrastructure, _functionRepo);
    FunctionGraph functionGraph = graphModel.build(_infrastructure);
    return new View(functionGraph, targets, listener, _executor);
  }

  //----------------------------------------------------------
  public static class View {

    private final FunctionGraph _graph;
    private final Collection<? extends PositionOrTrade> _inputs;
    private final Listener _listener;
    private final ExecutorService _executor;

    private View(FunctionGraph graph, Collection<? extends PositionOrTrade> inputs, Listener listener, ExecutorService executor) {
      _inputs = inputs;
      _listener = listener;
      _graph = graph;
      _executor = executor;
    }

    public void run(List<ViewColumn> columns) {
      List<Task> tasks = Lists.newArrayList();
      for (ViewColumn column : columns) {
        String columnName = column.getName();
        Map<ObjectId, InvokableFunction> functions = _graph.getFunctionsForColumn(columnName);
        for (PositionOrTrade input : _inputs) {
          ObjectId inputId = input.getUniqueId().getObjectId();
          ColumnOutput columnOutput = column.getOutput(input.getClass());
          FunctionConfig functionConfig = columnOutput.getFunctionConfig();
          InvokableFunction function = functions.get(inputId);
          FunctionArguments args = functionConfig.getFunctionArguments(function.getReceiver().getClass());
          tasks.add(new Task(input, args.getArguments(), columnName, function));
        }
      }
      List<Future<TaskResult>> futures;
      try {
        futures = _executor.invokeAll(tasks);
      } catch (InterruptedException e) {
        throw new OpenGammaRuntimeException("Interrupted", e);
      }
      Results.Builder resultsBuilder = new Results.Builder();
      for (Future<TaskResult> future : futures) {
        try {
          // TODO this won't do as a long term solution, it will block indefinitely if a function blocks
          TaskResult result = future.get();
          resultsBuilder.add(result._columnName, result._targetId, result._result);
        } catch (InterruptedException | ExecutionException e) {
          s_logger.warn("Failed to get result from task", e);
        }
      }
      _listener.cycleComplete(resultsBuilder.build());
    }

    //----------------------------------------------------------
    private static class TaskResult {
      
      private final ObjectId _targetId;
      private final String _columnName;
      private final Object _result;

      private TaskResult(ObjectId targetId, String columnName, Object result) {
        _targetId = targetId;
        _columnName = columnName;
        _result = result;
      }
    }

    //----------------------------------------------------------
    private static class Task implements Callable<TaskResult> {

      private final UniqueIdentifiable _input;
      private final String _columnName;
      private final InvokableFunction _invokableFunction;
      private final Map<String, Object> _args;
      // TODO need the arguments for the class that provides the function implementation

      private Task(UniqueIdentifiable input, Map<String, Object> args, String columnName, InvokableFunction invokableFunction) {
        _input = input;
        _args = args;
        _columnName = columnName;
        _invokableFunction = invokableFunction;
      }

      @Override
      public TaskResult call() throws Exception {
        Object result;
        try {
          result = _invokableFunction.invoke(_input, _args);
        } catch (Exception e) {
          s_logger.warn("Failed to execute function", e);
          result = e;
        }
        return new TaskResult(_input.getUniqueId().getObjectId(), _columnName, result);
      }
    }
  }
}
