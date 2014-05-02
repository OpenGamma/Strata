/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Common interface for bond future option calculators.
 */
public interface BondFutureOptionCalculator {

  /**
   * Calculates the present value of the bond future option.
   * @return the present value of the bond future option.
   */
  Result<MultipleCurrencyAmount> calculatePV();
  
  /**
   * Calculates the PV01 of the bond future option.
   * @return the PV01 of the bond future option.
   */
  Result<MultipleCurrencyMulticurveSensitivity> calculatePV01();
  
  /**
   * Calculates the model price of the bond future option.
   * @return the model price of the bond future option.
   */
  Result<Double> calculateModelPrice();
  
  /**
   * Calculates the delta of the bond future option.
   * @return the delta of the bond future option.
   */
  Result<Double> calculateDelta();
  
  /**
   * Calculates the gamma of the bond future option.
   * @return the gamma of the bond future option.
   */
  Result<Double> calculateGamma();
  
  /**
   * Calculates the vega of the bond future option.
   * @return the vega of the bond future option.
   */
  Result<Double> calculateVega();
  
  /**
   * Calculates the theta of the bond future option.
   * @return the theta of the bond future option.
   */
  Result<Double> calculateTheta();
}
