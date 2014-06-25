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

  Result<MultipleCurrencyAmount> calculatePV();
  
  Result<Double> calculateParRate();
  
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01();
  
  Result<Double> getSecurityMarketPrice();
  
  Result<Double> calculateSecurityModelPrice();
  
  Result<BucketedCurveSensitivities> calculateBucketedZeroIRDelta();
  
}
