/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.DefaultImplementationProvider;

/**
 * TODO interface?
 */
public class GraphConfig {

  public static final GraphConfig EMPTY = new GraphConfig(FunctionConfig.EMPTY);

  private final FunctionConfig _functionConfig;
  private final DefaultImplementationProvider _defaultImplementationProvider;
  private final ComponentMap _components;

  public GraphConfig(Object input,
                     ViewColumn column,
                     DefaultImplementationProvider defaultImplementationProvider,
                     ComponentMap components) {
    _defaultImplementationProvider = defaultImplementationProvider;
    _functionConfig = column.getFunctionConfig(input.getClass());
    _components = components;
  }

  public GraphConfig(FunctionConfig functionConfig,
                     DefaultImplementationProvider defaultImplementationProvider,
                     ComponentMap components) {
    _defaultImplementationProvider = defaultImplementationProvider;
    _functionConfig = functionConfig;
    _components = components;
  }

  public GraphConfig(FunctionConfig functionConfig) {
    _defaultImplementationProvider = new DefaultImplementationProvider() {
      @Override
      public Class<?> getDefaultImplementationType(Class<?> interfaceType) {
        return null;
      }
    };
    _functionConfig = functionConfig;
    _components = ComponentMap.EMPTY;
  }

  public Object getConstructorArgument(Class<?> objectType, Class<?> parameterType, String name) {
    FunctionArguments args = _functionConfig.getFunctionArguments(objectType);
    if (args == null) {
      return null;
    }
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
    Class<?> implType = _functionConfig.getFunctionImplementation(interfaceType);
    if (implType != null) {
      return implType;
    } else {
      return _defaultImplementationProvider.getDefaultImplementationType(interfaceType);
    }
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
