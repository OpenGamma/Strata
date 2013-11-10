/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collection;
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
import com.opengamma.util.ArgumentChecker;

/**
 * TODO this is totally provisional, just enough to run some basic tests that stitch everything together
 */
public class Engine {

  private static final Logger s_logger = LoggerFactory.getLogger(Engine.class);

  private final ExecutorService _executor;
  private final ComponentMap _components;
  private final GraphBuilder _graphBuilder;

  /* package */ Engine(ExecutorService executor, FunctionRepo functionRepo) {
    this(executor, ComponentMap.EMPTY, functionRepo, FunctionConfig.EMPTY, NodeDecorator.IDENTITY);
  }

  public Engine(ExecutorService executor,
                ComponentMap components,
                FunctionRepo functionRepo,
                FunctionConfig defaultConfig,
                NodeDecorator nodeDecorator) {
    _executor = ArgumentChecker.notNull(executor, "executor");
    _components = ArgumentChecker.notNull(components, "components");
    // TODO pass this in as an argument?
    _graphBuilder = new GraphBuilder(functionRepo, components, defaultConfig, nodeDecorator);
  }

  /*public interface Listener {

    void cycleComplete(Results results);
  }*/

  // TODO allow targets to be anything? would allow support for parallelization, e.g. List<SwapSecurity>
  // might have to make target type an object instead of a type param on OutputFunction to cope with erasure
  public View createView(ViewDef viewDef, Collection<? extends PositionOrTrade> targets) {
    GraphModel graphModel = _graphBuilder.build(viewDef, targets);
    Graph graph = graphModel.build(_components);
    return new View(viewDef, graph, targets, _executor);
  }

  //----------------------------------------------------------
  public static class View {

    private final Graph _graph;
    private final ViewDef _viewDef;
    private final Collection<? extends PositionOrTrade> _inputs;
    private final ExecutorService _executor;

    private View(ViewDef viewDef,
                 Graph graph,
                 Collection<? extends PositionOrTrade> inputs,
                 ExecutorService executor) {
      _viewDef = viewDef;
      _inputs = inputs;
      _graph = graph;
      _executor = executor;
    }

    public Results run() {
      List<Task> tasks = Lists.newArrayList();
      for (ViewColumn column : _viewDef.getColumns()) {
        String columnName = column.getName();
        Map<ObjectId, InvokableFunction> functions = _graph.getFunctionsForColumn(columnName);
        for (PositionOrTrade input : _inputs) {
          ObjectId inputId = input.getUniqueId().getObjectId();
          InvokableFunction function = functions.get(inputId);
          FunctionConfig functionConfig = column.getFunctionConfig(input.getClass());
          FunctionArguments args = functionConfig.getFunctionArguments(function.getReceiver().getClass());
          tasks.add(new Task(input, args, columnName, function));
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
      return resultsBuilder.build();
    }

    // TODO run() variants that take:
    //   1) cycle and listener (for multiple execution)
    //   2) listener (for single async or infinite execution)

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
      private final FunctionArguments _args;
      // TODO need the arguments for the class that provides the function implementation

      private Task(UniqueIdentifiable input, FunctionArguments args, String columnName, InvokableFunction invokableFunction) {
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
