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

  Result<MultipleCurrencyAmount> calculatePV();
  
  Result<MultipleCurrencyMulticurveSensitivity> calculatePV01();
}
