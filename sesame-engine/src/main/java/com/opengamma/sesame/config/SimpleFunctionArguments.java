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
public final class SimpleFunctionArguments implements FunctionArguments {

  /**
   * User-specified function arguments keyed by parameter name.
   * TODO should these be the real objects or the string representation?
   */
  private final Map<String, Object> _arguments;

  public SimpleFunctionArguments(Map<String, Object> arguments) {
    _arguments = ImmutableMap.copyOf(ArgumentChecker.notNull(arguments, "arguments"));
  }

  @Override
  public Object getArgument(String parameterName) {
    return _arguments.get(parameterName);
  }

  @Override
  public String toString() {
    return "SimpleFunctionArguments [" +
        "_arguments=" + _arguments +
        "]";
  }
}
