/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class FunctionConfig {

  public static final FunctionConfig EMPTY =
      new FunctionConfig(Collections.<Class<?>, Class<?>>emptyMap(),
                         Collections.<Class<?>, FunctionArguments>emptyMap());

  /** Map of function types to their implementing classes where the implementing class isn't the default. */
  private final Map<Class<?>, Class<?>> _implementationOverrides;

  /** User-specified function arguments, keyed by the function implementation type. */
  private final Map<Class<?>, FunctionArguments> _arguments;

  public FunctionConfig(Map<Class<?>, Class<?>> implementationOverrides, Map<Class<?>, FunctionArguments> arguments) {
    ArgumentChecker.notNull(implementationOverrides, "implementationOverrides");
    ArgumentChecker.notNull(arguments, "arguments");
    _implementationOverrides = ImmutableMap.copyOf(implementationOverrides);
    _arguments = ImmutableMap.copyOf(arguments);
  }

  public Class<?> getFunctionImplementation(Class<?> functionInterface) {
    return _implementationOverrides.get(functionInterface);
  }

  public FunctionArguments getFunctionArguments(Class<?> functionImplementationType) {
    FunctionArguments functionArguments = _arguments.get(functionImplementationType);
    if (functionArguments == null) {
      return FunctionArguments.EMPTY;
    } else {
      return functionArguments;
    }
  }
}
