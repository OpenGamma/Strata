/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Calculator initialised with the data required to perform
 * analytics calculations for a particular security.
 */
public interface IFRACalculator {

  /**
   * Calculates the present value for the security
   *
   * @return the present value
   */
  Result<MultipleCurrencyAmount> calculatePV();

  /**
   * Calculates the par rate for the security
   *
   * @return the par rate
   */
  Result<Double> calculateRate();


}
