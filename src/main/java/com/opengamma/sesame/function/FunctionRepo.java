/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Set;

/**
 * Repository for engine functions. This returns the function type that can satisfy a value name for a target type.
 */
public interface FunctionRepo {

  // for when the user is configuring a column with a default output name. this shows which types it can handle
  Set<Class<?>> getInputTypes(String outputName);

  // for when the user is configuring a column
  Set<String> getAvailableOutputs(Class<?> inputType);

  // for when the user is configuring outputs that aren't derived from the portfolio
  Set<String> getAvailableOutputs();

  // function for outputs that take an input from the portfolio
  FunctionMetadata getOutputFunction(String outputName, Class<?> inputType);

  // function for outputs that have no parameters or only use configuration parameters
  FunctionMetadata getOutputFunction(String outputName);

  // TODO do these 2 methods really belong in the function repo?
  // they're more about building than specifically about functions
  // should they be somewhere else along with provider registration?
  // wherever they go GraphConfig will need to be involved
  Class<?> getDefaultImplementation(Class<?> interfaceType);

  // gives the available implementing types for function interfaces
  // these can be presented to the user when they're setting up the view and choosing implementation overrides
  Set<Class<?>> getImplementationTypes(Class<?> interfaceType);

}
