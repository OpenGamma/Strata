/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Basic function configuration implementation providing implementations and arguments.
 */
public final class SimpleFunctionModelConfig implements FunctionModelConfig {

  /**
   * The function implementation classes keyed by function interface.
   * This only needs to be populated if the implementation is not the default.
   */
  private final ImmutableMap<Class<?>, Class<?>> _implementationOverrides;
  /**
   * The user-specified function arguments keyed by function implementation.
   */
  private final ImmutableMap<Class<?>, FunctionArguments> _arguments;

  /**
   * Creates an instance.
   * 
   * @param implementationOverrides  the map of function implementation keyed by function interface, not null
   * @param arguments  the map of arguments keyed by function implementation, not null
   */
  public SimpleFunctionModelConfig(Map<Class<?>, Class<?>> implementationOverrides,
                                   Map<Class<?>, FunctionArguments> arguments) {
    _implementationOverrides = ImmutableMap.copyOf(ArgumentChecker.notNull(implementationOverrides, "implementationOverrides"));
    _arguments = ImmutableMap.copyOf(ArgumentChecker.notNull(arguments, "arguments"));
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<?> getFunctionImplementation(Class<?> functionType) {
    return _implementationOverrides.get(functionType);
  }

  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    FunctionArguments functionArguments = _arguments.get(functionType);
    if (functionArguments == null) {
      return FunctionArguments.EMPTY;
    } else {
      return functionArguments;
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "SimpleFunctionConfig [" +
        ", _arguments=" + _arguments +
        "_implementationOverrides=" + _implementationOverrides +
        "]";
  }

}
