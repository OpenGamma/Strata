/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Factory for creating a calculator for a InterestRateSwap.
 */
public interface IInterestRateSwapCalculatorFactory {

  /**
   * Creates the calculator for the supplied InterestRateSwap.
   *
   * @param env the current environment, not null
   * @param security the fra to create a calculator
   * for, not null
   * @return result containing the calculator if successfully
   * created, a failure result otherwise
   */
  Result<InterestRateSwapCalculator> createCalculator(Environment env, InterestRateSwapSecurity security);
}
