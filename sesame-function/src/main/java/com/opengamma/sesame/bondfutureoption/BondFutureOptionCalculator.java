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
}
