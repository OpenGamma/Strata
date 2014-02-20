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
 *
 */
public class DefaultImplementationProvider implements FunctionModelConfig {

  private final AvailableImplementations _availableImplementations;

  public DefaultImplementationProvider(AvailableImplementations availableImplementations) {
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "functionRepo");
  }

  /**
   * Returns the default implementation from some {@link AvailableImplementations}.
   * @param functionType The interface
   * @return The implementation from {@link AvailableImplementations#getDefaultImplementation}
   */
  @Override
  public Class<?> getFunctionImplementation(Class<?> functionType) {
    return _availableImplementations.getDefaultImplementation(functionType);
  }

  /**
   * @return {@link FunctionArguments#EMPTY}
   */
  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    return FunctionArguments.EMPTY;
  }
}
