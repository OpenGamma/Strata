/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Common interface for interest rate future option calculators.
 */
public interface IRFutureOptionCalculator {

  /**
   * Calculates the present value of the interest rate future option.
   * @return the present value.
   */
  Result<MultipleCurrencyAmount> calculatePV();
  
  /**
   * Calculates the PV01 of the interest rate future option.
   * @return the PV01.
   */
  Result<MultipleCurrencyMulticurveSensitivity> calculatePV01();
}
