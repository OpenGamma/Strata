/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.util.result.Result;
/**
 * Deliverable swap future function to calculate PV, PV01 etc.
 */
public interface DeliverableSwapFutureFn {

  /**
   * Calculates the future contract price using a curve.
   * @param env the environment that the future contract price will be calculated with.
   * @param delivSwapFutureTrade the interest rate future trade to calculate the future contract price for.
   * @return result containing the future contract price.
   */
  @Output(value = OutputNames.SECURITY_MODEL_PRICE)
  Result<Double> calculateSecurityModelPrice(Environment env, DeliverableSwapFutureTrade delivSwapFutureTrade);
  
}