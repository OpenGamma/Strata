/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import javax.annotation.Nullable;
import javax.inject.Provider;

import com.opengamma.sesame.function.Parameter;

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
  FunctionModelConfig EMPTY = EmptyFunctionModelConfig.getInstance();

  //-------------------------------------------------------------------------
  /**
   * Gets the implementation that should be used for creating instances of a type.
   * <p>
   * The result implementation can be:
   * <ul>
   *   <li>An implementation of an interface</li>
   *   <li>A {@link Provider} that can provide the implementation</li>
   * </ul>
   * 
   * @param functionType  the type to lookup, not null
   * @param parameter the constructor parameter for which an implementation is required
   * @return the implementation that should be used, null if unknown
   */
  Class<?> getFunctionImplementation(Class<?> functionType, @Nullable Parameter parameter);

  /**
   * Gets the arguments for a function.
   * 
   * @param functionType  the type of function, not null
   * @return the arguments, empty if not found, not null
   */
  FunctionArguments getFunctionArguments(Class<?> functionType);
}
