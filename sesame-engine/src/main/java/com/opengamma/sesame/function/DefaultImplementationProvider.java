/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.util.ArgumentChecker;

/**
 * Function model configuration providing default implementations.
 */
public class DefaultImplementationProvider implements FunctionModelConfig {

  /**
   * The available implementations.
   */
  private final AvailableImplementations _availableImplementations;

  /**
   * Creates an instance.
   * 
   * @param availableImplementations  the available implementations to use, not null
   */
  public DefaultImplementationProvider(AvailableImplementations availableImplementations) {
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "functionRepo");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the default implementation from some {@link AvailableImplementations}.
   * 
   * @param functionType  the function type to get the implementation for, not null
   * @return the implementation from {@link AvailableImplementations#getDefaultImplementation}
   */
  @Override
  public Class<?> getFunctionImplementation(Class<?> functionType) {
    return _availableImplementations.getDefaultImplementation(functionType);
  }

  /**
   * Returns null.
   *
   * @param parameter ignored
   * @return null
   */
  @Override
  public Class<?> getFunctionImplementation(Parameter parameter) {
    return null;
  }

  /**
   * Gets the function arguments by type, which returns the empty arguments.
   * 
   * @param functionType  the function type to get arguments for, not null
   * @return {@link FunctionArguments#EMPTY}
   */
  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    return FunctionArguments.EMPTY;
  }

}
