/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * TODO should this be called OutputMetadata?
 */
public class FunctionMetadata {

  private final Method _method;
  // TODO mapping from method args to params - how will this work?
  // inputs are a target and map of param names to arg values
  // need map of param name to index
  // index of target param - can this just be any other parameter?
  // if we don't have a target annotation how can we figure
  // the invoker needs this
  // need to present the required args to the user in the UI
  // need to know the param names and types
  private final Map<String, Parameter> _parameters;
  // TODO invoker
}
