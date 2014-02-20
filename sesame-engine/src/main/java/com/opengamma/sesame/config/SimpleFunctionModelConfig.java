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
 *
 */
public final class SimpleFunctionModelConfig implements FunctionModelConfig {

  /** Map of function types to their implementing classes where the implementing class isn't the default. */
  private final Map<Class<?>, Class<?>> _implementationOverrides;

  /** User-specified function arguments, keyed by the function implementation type. */
  private final Map<Class<?>, FunctionArguments> _arguments;

  public SimpleFunctionModelConfig(Map<Class<?>, Class<?>> implementationOverrides,
                                   Map<Class<?>, FunctionArguments> arguments) {
    _implementationOverrides = ImmutableMap.copyOf(ArgumentChecker.notNull(implementationOverrides, "implementationOverrides"));
    _arguments = ImmutableMap.copyOf(ArgumentChecker.notNull(arguments, "arguments"));
  }

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

  @Override
  public String toString() {
    return "SimpleFunctionConfig [" +
        ", _arguments=" + _arguments +
        "_implementationOverrides=" + _implementationOverrides +
        "]";
  }
}
