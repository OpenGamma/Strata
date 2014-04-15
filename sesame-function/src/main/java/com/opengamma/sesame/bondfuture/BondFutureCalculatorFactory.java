/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfuture;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.BondFutureTrade;
import com.opengamma.util.result.Result;

/**
 * Sesame engine function interface for creating calculators for bond futures.
 */
public interface BondFutureCalculatorFactory {

  /**
   * Returns a calculator for a specified environment and bond future.
   * @param env the environment.
   * @param trade the bond future trade.
   * @return a calculator for bond futures.
   */
  Result<BondFutureDiscountingCalculator> createCalculator(Environment env, BondFutureTrade trade);
}
