/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import org.apache.commons.lang.ClassUtils;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.graph.Node;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class GraphConfig {

  public static final GraphConfig EMPTY = new GraphConfig(FunctionConfig.EMPTY);

  private final FunctionConfig _functionConfig;
  private final ComponentMap _components;
  private final NodeDecorator _nodeDecorator;

  public GraphConfig(FunctionConfig functionConfig, ComponentMap components, NodeDecorator nodeDecorator) {
    _functionConfig = ArgumentChecker.notNull(functionConfig, "functionConfig");
    _components = ArgumentChecker.notNull(components, "components");
    _nodeDecorator = ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
  }

  public GraphConfig(FunctionConfig functionConfig) {
    _functionConfig = functionConfig;
    _components = ComponentMap.EMPTY;
    _nodeDecorator = NodeDecorator.IDENTITY;
  }

  public Object getConstructorArgument(Class<?> objectType, Parameter parameter) {
    FunctionArguments args = _functionConfig.getFunctionArguments(objectType);
    Object arg = args.getArgument(parameter.getName());
    if (arg == null) {
      return null;
    // this takes into account boxing of primitives which Class.isAssignableFrom() doesn't
    } else if (ClassUtils.isAssignable(arg.getClass(), parameter.getType(), true)) {
      return arg;
    } else {
      throw new IllegalArgumentException("Argument (" + arg + ": " + arg.getClass().getSimpleName() + ") isn't of the " +
                                             "required type for " + parameter.getFullName());
    }
  }

  public Class<?> getImplementationType(Class<?> interfaceType) {
    return _functionConfig.getFunctionImplementation(interfaceType);
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
