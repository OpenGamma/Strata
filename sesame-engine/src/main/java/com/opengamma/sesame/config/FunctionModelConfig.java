/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import javax.inject.Provider;

/**
 * Configuration for individual functions in the function model.
 * <p>
 * Provides the implementation types for function interfaces and the
 * arguments for creating function instances.
 */
public interface FunctionModelConfig {

  /**
   * Singleton instance of an empty configuration.
   * Always returns a null implementation class and empty arguments.
   */
  FunctionModelConfig EMPTY = new FunctionModelConfig() {
    @Override
    public Class<?> getFunctionImplementation(Class<?> functionType) {
      return null;
    }

    @Override
    public FunctionArguments getFunctionArguments(Class<?> functionType) {
      return FunctionArguments.EMPTY;
    }
  };

  //-------------------------------------------------------------------------
  /**
   * Gets the implementation that should be used for creating instances of a type.
   * <p>
   * The result implementation can be:
   * <ul>
   * <li>An implementation of an interface</li>
   * <li>A {@link Provider} that can provide the implementation</li>
   * </ul>
   * 
   * @param functionType  the type to lookup, not null
   * @return the implementation that should be used, null if unknown
   */
  Class<?> getFunctionImplementation(Class<?> functionType);

  /**
   * Gets the arguments for a function.
   * 
   * @param functionType  the type of function, not null
   * @return the arguments, empty if not found, not null
   */
  FunctionArguments getFunctionArguments(Class<?> functionType);

}
