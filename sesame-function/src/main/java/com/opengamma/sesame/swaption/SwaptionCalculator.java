/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import com.opengamma.analytics.financial.interestrate.PresentValueSABRSensitivityDataBundle;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculator initialised with the data required to perform
 * analytics calculations for a particular security.
 */
public interface SwaptionCalculator {

  /**
   * Calculates the present value for the security
   *
   * @return the present value
   */
  Result<MultipleCurrencyAmount> calculatePV();

  /**
   * Calculates the implied volatility for the security
   *
   * @return the implied volatility
   */
  Result<Double> calculateImpliedVolatility();

  /**
   * Calculates the PV01 for the security
   *
   * @return the PV01
   */
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01();

  /**
   * Calculates the bucketed PV01 for the security
   *
   * @return the bucketed PV01
   */
  Result<MultipleCurrencyParameterSensitivity> calculateBucketedPV01();

  /**
   * Calculates the bucketed SABR risk for the security
   *
   * @return the bucketed SABR risk
   */
  Result<PresentValueSABRSensitivityDataBundle> calculateBucketedSABRRisk();
}
