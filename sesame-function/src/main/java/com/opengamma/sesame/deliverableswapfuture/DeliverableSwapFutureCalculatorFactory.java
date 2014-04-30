/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.util.result.Result;

/**
 * Common interface for creating calculators for deliverable swap futures.
 */
public interface DeliverableSwapFutureCalculatorFactory {

  /**
   * Returns a calculator for a specified environment and deliverable swap future.
   * 
   * @param env the environment.
   * @param trade the deliverable interest rate future trade.
   * @return a calculator for deliverable swap futures.
   */
  Result<DeliverableSwapFutureCalculator> createCalculator(Environment env, DeliverableSwapFutureTrade trade);
  
}
