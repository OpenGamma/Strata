/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import javax.inject.Provider;

import org.apache.commons.lang.ClassUtils;

import com.opengamma.core.link.Link;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.Node;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.util.ArgumentChecker;

/**
 * Configuration for creating a model of the graph of functions.
 * Contains configuration for:
 * <ul>
 *   <li>The individual functions ({@link FunctionModel})
 *   <li>The components provided by the engine ({@link ComponentMap})
 *   <li>The decorators that insert proxies between the functions to provide engine services ({@link NodeDecorator})
 * </ul>
 */
public class GraphConfig {

  public static final GraphConfig EMPTY = new GraphConfig(FunctionModelConfig.EMPTY);

  private final FunctionModelConfig _functionModelConfig;
  private final ComponentMap _components;
  private final NodeDecorator _nodeDecorator;

  public GraphConfig(FunctionModelConfig functionModelConfig, ComponentMap components, NodeDecorator nodeDecorator) {
    _functionModelConfig = ArgumentChecker.notNull(functionModelConfig, "functionConfig");
    _components = ArgumentChecker.notNull(components, "components");
    _nodeDecorator = ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
  }

  public GraphConfig(FunctionModelConfig functionModelConfig) {
    _functionModelConfig = functionModelConfig;
    _components = ComponentMap.EMPTY;
    _nodeDecorator = NodeDecorator.IDENTITY;
  }

  // TODO where does this logic belong? FunctionArguments? it doesn't use any state from here
  public Object getConstructorArgument(Class<?> objectType, Parameter parameter) {
    FunctionArguments args = _functionModelConfig.getFunctionArguments(objectType);
    Object arg = args.getArgument(parameter.getName());
    if (arg == null) {
      return null;
    // this takes into account boxing of primitives which Class.isAssignableFrom() doesn't
    } else if (ClassUtils.isAssignable(arg.getClass(), parameter.getType(), true)) {
      return arg;
    } else if (arg instanceof Provider) {
      // todo - could we make the type check stronger here?
      return arg;
    } else if (arg instanceof Link) {
      if (ClassUtils.isAssignable(((Link) arg).getType(), parameter.getType(), true)) {
        return arg;
      } else {
        throw new IllegalArgumentException("Link argument (" + arg + ") doesn't resolve to the " +
                                               "required type for " + parameter.getFullName());
      }
    } else {
      throw new IllegalArgumentException("Argument (" + arg + ": " + arg.getClass().getSimpleName() + ") isn't of the " +
                                             "required type for " + parameter.getFullName());
    }
  }

  public Class<?> getImplementationType(Class<?> interfaceType) {
    return _functionModelConfig.getFunctionImplementation(interfaceType);
  }

  public Object getComponent(Class<?> type) {
    return _components.findComponent(type);
  }

  // TODO should this be on a separate interface? the others are used in graph building, this is for execution
  // arguments for the method that implements the function and returns the output
  // it overlaps with getConstructorArgument because the args are all in one bucket
  // args are derived from system defaults and view def config
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    throw new UnsupportedOperationException();
  }

  public ComponentMap getComponents() {
    return _components;
  }

  public Node decorateNode(Node node) {
    return _nodeDecorator.decorateNode(node);
  }
}
