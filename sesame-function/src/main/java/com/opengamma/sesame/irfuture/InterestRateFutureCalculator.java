/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Common interface for interest rate future calculators.
 */
public interface InterestRateFutureCalculator {
  
  /**
   * Calculates the PV for the security from the given curve
   * @return result containing the PV if successfully created, a failure result otherwise
   */
  Result<MultipleCurrencyAmount> calculatePV();
  
  /**
   * Calculates the (fair) forward rate used for calculation in the PV
   * @return result containing the par rate if successfully created, a failure result otherwise
   */
  Result<Double> calculateParRate();
  
  /**
   * Calculates the PV01 for the security
   *
   * @return result containing the PV01 if successfully created, a failure result otherwise
   */
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01();
  
  /**
   * Calculates the market price for the security
   *
   * @return result containing the market price if successfully created, a failure result otherwise
   */
  Result<Double> getSecurityMarketPrice();
  
  /**
   * Calculates the theoretical price for the security
   *
   * @return result containing the theoretical price if successfully created, a failure result otherwise
   */
  Result<Double> calculateSecurityModelPrice();
  
  /**
   * Calculates the bucketed zero rate delta for the security
   *
   * @return the bucketed delta
   */
  Result<BucketedCurveSensitivities> calculateBucketedZeroIRDelta();
  
}
