/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
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
  
  /**
   * Calculates the model price of the interest rate future option.
   * @return the model price.
   */
  Result<Double> calculateModelPrice();
  
  /**
   * Calculate the delta of the interest rate future option.
   * @return the delta.
   */
  Result<Double> calculateDelta();
  
  /**
   * Calculate the gamma of the interest rate future option.
   * @return the gamma.
   */
  Result<Double> calculateGamma();
  
  /**
   * Calculate the vega of the interest rate future option.
   * @return the vega.
   */
  Result<Double> calculateVega();
  
  /**
   * Calculate the theta of the interest rate future option.
   * @return the theta.
   */
  Result<Double> calculateTheta();
  
  /**
   * Calculate the bucketed ir delta of the interest rate future option w.r.t zero rates.
   * @return the BucketedCurveSensitivities view of the sensitivities.
   */
  Result<BucketedCurveSensitivities> calculateBucketedZeroIRDelta();
}
