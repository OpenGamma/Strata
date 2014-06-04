/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Set;

import com.opengamma.DataNotFoundException;
import com.opengamma.sesame.OutputName;

/**
 *
 */
public interface AvailableOutputs {

  // for when the user is configuring a column with a default output name. this shows which types it can handle
  Set<Class<?>> getInputTypes(OutputName outputName);

  // all types accepted as inputs by registered functions
  Set<Class<?>> getInputTypes();

  // for when the user is configuring a column
  Set<OutputName> getAvailableOutputs(Class<?> inputType);

  // for when the user is configuring outputs that aren't derived from the portfolio
  Set<OutputName> getAvailableOutputs();

  /**
   * Returns metadata for the function that provides an output for an input type.
   * An output is provided by a method annotated with {@link Output}.
   * @param outputName The output name
   * @param inputType The type of the input
   * @return The function that can provide the output
   * @throws DataNotFoundException If nothing can provide the requested output for the target type
   * TODO should this return multiple functions?
   */
  FunctionMetadata getOutputFunction(OutputName outputName, Class<?> inputType);


  /**
   * Returns metadata for a functions that provide an output that isn't calculated for items in the portfolio.
   * e.g. curves, surfaces or other intermediate values
   * @param outputName The output name
   * @return The function that can provide the output
   * TODO should this return multiple functions?
   */
  FunctionMetadata getOutputFunction(OutputName outputName);

  /**
   * Registers functions that can produce outputs from methods annotated with {@link Output}.
   * @param functionInterfaces The function types to register
   */
  void register(Class<?>... functionInterfaces);
}
