/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.InterestRateFutureTrade;
import com.opengamma.util.result.Result;

/**
 * Common interface for creating calculators for interest rate futures.
 */
public interface InterestRateFutureCalculatorFactory {

  /**
   * Returns a calculator for a specified environment and interest rate future.
   * 
   * @param env the environment.
   * @param trade the interest rate future trade.
   * @return a calculator for interest rate futures.
   */
  Result<InterestRateFutureCalculator> createCalculator(Environment env, InterestRateFutureTrade trade);
}
