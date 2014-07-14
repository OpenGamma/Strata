/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Bond calculator.
 */
public interface BondCalculator {

  /**
   * Calculates the present value for the security
   *
   * @return result containing the PV if successfully created, a failure result otherwise
   */
  Result<MultipleCurrencyAmount> calculatePV();

}
