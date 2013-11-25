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
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.sesame.trace.NoOpTracer;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
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
    s_logger.debug("building graph model");
    GraphModel graphModel = _graphBuilder.build(viewDef, targets);
    s_logger.debug("graph model complete, building graph");
    Graph graph = graphModel.build(_components);
    s_logger.debug("graph complete");
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
      int colIndex = 0;
      List<ViewColumn> columns = _viewDef.getColumns();
      List<String> columnNames = Lists.newArrayListWithCapacity(columns.size());
      for (ViewColumn column : columns) {
        String columnName = column.getName();
        columnNames.add(columnName);
        Map<Class<?>, InvokableFunction> functions = _graph.getFunctionsForColumn(columnName);
        int rowIndex = 0;
        for (PositionOrTrade input : _inputs) {
          InvokableFunction function;
          InvokableFunction posOrTradeFunction = functions.get(input.getClass());
          if (posOrTradeFunction != null) {
            function = posOrTradeFunction;
          } else {
            function = functions.get(input.getSecurity().getClass());
          }
          FunctionConfig functionConfig = column.getFunctionConfig(input.getClass());
          FunctionArguments args = functionConfig.getFunctionArguments(function.getReceiver().getClass());
          // TODO this needs to be configurable. if we don't have IDs how will we signal which inputs to trace? row/col index?
          Tracer tracer = NoOpTracer.INSTANCE;
          //Tracer tracer = new FullTracer();
          tasks.add(new Task(input, args, rowIndex++, colIndex, function, tracer));
        }
        colIndex++;
      }
      List<Future<TaskResult>> futures;
      try {
        futures = _executor.invokeAll(tasks);
      } catch (InterruptedException e) {
        throw new OpenGammaRuntimeException("Interrupted", e);
      }
      Results.Builder resultsBuilder = Results.builder(columnNames);
      for (Future<TaskResult> future : futures) {
        try {
          // TODO this won't do as a long term solution, it will block indefinitely if a function blocks
          TaskResult result = future.get();
          resultsBuilder.add(result._rowIndex, result._columnIndex, result._input, result._result, result._callGraph);
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
      
      private final Object _input;
      private final int _rowIndex;
      private final int _columnIndex;
      private final Object _result;
      private final CallGraph _callGraph;

      private TaskResult(Object input, int rowIndex, int columnIndex, Object result, CallGraph callGraph) {
        _input = input;
        _rowIndex = rowIndex;
        _columnIndex = columnIndex;
        _result = result;
        _callGraph = callGraph;
      }
    }

    //----------------------------------------------------------
    private static class Task implements Callable<TaskResult> {

      private final UniqueIdentifiable _input;
      private final int _rowIndex;
      private final int _columnIndex;
      private final InvokableFunction _invokableFunction;
      private final Tracer _tracer;
      private final FunctionArguments _args;

      private Task(UniqueIdentifiable input,
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
        return new TaskResult(_input, _rowIndex, _columnIndex, result, callGraph);
      }
    }
  }
}
