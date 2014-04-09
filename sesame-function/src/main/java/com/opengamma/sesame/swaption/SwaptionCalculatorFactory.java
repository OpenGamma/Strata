/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Factory for creating a calculator for a swaption.
 */
public interface SwaptionCalculatorFactory {

  /**
   * Creates the calculator for the supplied swaption.
   *
   * @param env the current environment, not null
   * @param security the swaption to create a calculator
   * for, not null
   * @return result containing the calculator if successfully
   * created, a failure result otherwise
   */
  Result<SwaptionCalculator> createCalculator(Environment env, SwaptionSecurity security);
}
