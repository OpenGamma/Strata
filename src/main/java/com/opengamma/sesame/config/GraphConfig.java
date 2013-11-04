/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.graph.Node;
import com.opengamma.sesame.proxy.NodeDecorator;
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
    ArgumentChecker.notNull(functionConfig, "functionConfig");
    ArgumentChecker.notNull(components, "components");
    ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
    _functionConfig = functionConfig;
    _components = components;
    _nodeDecorator = nodeDecorator;
  }

  public GraphConfig(FunctionConfig functionConfig) {
    _functionConfig = functionConfig;
    _components = ComponentMap.EMPTY;
    _nodeDecorator = NodeDecorator.IDENTITY;
  }

  public Object getConstructorArgument(Class<?> objectType, Class<?> parameterType, String name) {
    FunctionArguments args = _functionConfig.getFunctionArguments(objectType);
    Object arg = args.getArgument(name);
    if (arg == null) {
      return null;
    } else if (parameterType.isInstance(arg)) {
      return arg;
    } else {
      throw new IllegalArgumentException("Argument " + arg + " isn't of the required type " + parameterType.getName());
    }
  }

  public Class<?> getImplementationType(Class<?> interfaceType) {
    return _functionConfig.getFunctionImplementation(interfaceType);
  }

  public Object getObject(Class<?> type) {
    return _components.getComponent(type);
  }

  // TODO should this be on a separate interface? the others are used in graph building, this is for execution
  // arguments for the method that implements the function and returns the output
  // it overlaps with getConstructorArgument because the args are all in one bucket
  // args are derived from system defaults and view def config
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    throw new UnsupportedOperationException();
  }

  public Node decorateNode(Node node) {
    return _nodeDecorator.decorateNode(node);
  }
}
