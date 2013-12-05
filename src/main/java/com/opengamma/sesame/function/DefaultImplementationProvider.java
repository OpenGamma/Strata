/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class DefaultImplementationProvider implements FunctionConfig {

  private final AvailableImplementations _availableImplementations;

  public DefaultImplementationProvider(AvailableImplementations availableImplementations) {
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "functionRepo");
  }

  /**
   * Returns the default implementation from some {@link AvailableImplementations}.
   * @param functionInterface The interface
   * @return The implementation from {@link AvailableImplementations#getDefaultImplementation}
   */
  @Override
  public Class<?> getFunctionImplementation(Class<?> functionInterface) {
    return _availableImplementations.getDefaultImplementation(functionInterface);
  }

  /**
   * @return {@link FunctionArguments#EMPTY}
   */
  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    return FunctionArguments.EMPTY;
  }
}
