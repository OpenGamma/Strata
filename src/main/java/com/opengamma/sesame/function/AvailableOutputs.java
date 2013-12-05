/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Set;

/**
 *
 */
public interface AvailableOutputs {

  // for when the user is configuring a column with a default output name. this shows which types it can handle
  Set<Class<?>> getInputTypes(String outputName);

  // for when the user is configuring a column
  Set<String> getAvailableOutputs(Class<?> inputType);

  // for when the user is configuring outputs that aren't derived from the portfolio
  Set<String> getAvailableOutputs();

  // TODO should this return multiple functions?
  // function for outputs that take an input from the portfolio
  FunctionMetadata getOutputFunction(String outputName, Class<?> inputType);

  // TODO should this return multiple functions?
  // function for outputs that have no parameters or only use configuration parameters
  FunctionMetadata getOutputFunction(String outputName);

  void register(Class<?>... types);
}
