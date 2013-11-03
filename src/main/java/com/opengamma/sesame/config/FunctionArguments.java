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
public final class FunctionArguments {

  public static final FunctionArguments EMPTY = new FunctionArguments(Collections.<String, Object>emptyMap());

  /**
   * User-specified function arguments keyed by parameter name.
   * TODO should these be the real objects or the string representation?
   */
  private final Map<String, Object> _arguments;

  public FunctionArguments(Map<String, Object> arguments) {
    ArgumentChecker.notNull(arguments, "arguments");
    _arguments = ImmutableMap.copyOf(arguments);
  }

  public Object getArgument(String parameterName) {
    return _arguments.get(parameterName);
  }

}
