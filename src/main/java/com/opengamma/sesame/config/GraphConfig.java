/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.sesame.engine.ComponentMap;

/**
 * TODO interface?
 */
public class GraphConfig {

  public static final GraphConfig EMPTY = new GraphConfig(FunctionConfig.EMPTY);

  private final FunctionConfig _functionConfig;
  private final ComponentMap _components;

  /*public GraphConfig(Object input,
                     ViewColumn column,
                     ComponentMap components) {
    _functionConfig = column.getFunctionConfig(input.getClass());
    _components = components;
  }*/

  public GraphConfig(FunctionConfig functionConfig, ComponentMap components) {
    _functionConfig = functionConfig;
    _components = components;
  }

  public GraphConfig(FunctionConfig functionConfig) {
    _functionConfig = functionConfig;
    _components = ComponentMap.EMPTY;
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
}
