/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

/**
 * Configuration for individual functions in the function model.
 * Provides the implementation types for function interfaces and the arguments for creating function instances.
 */
public interface FunctionModelConfig {

  /**
   * Always returns a null implementation class and empty arguments.
   */
  public static final FunctionModelConfig EMPTY = new FunctionModelConfig() {
    @Override
    public Class<?> getFunctionImplementation(Class<?> functionInterface) {
      return null;
    }

    @Override
    public FunctionArguments getFunctionArguments(Class<?> functionType) {
      return FunctionArguments.EMPTY;
    }
  };

  /**
   * Returns the implementation that should be used for an interface.
   * @param functionInterface The interface
   * @return The implementation that should be used, null if unknown
   */
  Class<?> getFunctionImplementation(Class<?> functionInterface);

  /**
   * Returns the arguments for a function.
   * @param functionType The type of function
   * @return The arguments, not null, but possibly empty
   */
  FunctionArguments getFunctionArguments(Class<?> functionType);
}
