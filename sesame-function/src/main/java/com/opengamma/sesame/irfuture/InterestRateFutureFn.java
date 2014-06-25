/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.trade.InterestRateFutureTrade;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Interest rate future function to calculate PV, par rate and PV01.
 */
public interface InterestRateFutureFn {

  /**
   * Calculate the par rate of the interest rate future.
   * 
   * @param env the environment that the par rate will be calculated with.
   * @param irFutureTrade interest rate future to calculate the par rate for.
   * @return result containing the par rate if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PAR_RATE)
  Result<Double> calculateParRate(Environment env, InterestRateFutureTrade irFutureTrade);

  /**
   * Calculate the present value of the interest rate future.
   *
   * @param env the environment that the PV will be calculated with.
   * @param irFutureTrade the interest rate future trade to calculate the PV for.
   * @return result containing the present value if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateFutureTrade irFutureTrade);
  
  /**
   * Calculate the PV01 for an interest rate future security.
   * 
   * @param env the environment that the PV01 will be calculated with.
   * @param irFutureTrade the interest rate future trade to calculate the PV for.
   * @return result containing the PV01 if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PV01)
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateFutureTrade irFutureTrade);
  
  /**
   * Returns the future contract reference price.
   * @param env the environment.
   * @param irFutureTrade the trade containing the interest rate future.
   * @return the future contract reference price.
   */
  @Output(value = OutputNames.SECURITY_MARKET_PRICE)
  Result<Double> getSecurityMarketPrice(Environment env, InterestRateFutureTrade irFutureTrade);
  
  /**
   * Calculates the future contract price using a curve.
   * @param env the environment that the future contract price will be calculated with.
   * @param irFutureTrade the interest rate future trade to calculate the future contract price for.
   * @return result containing the future contract price.
   */
  @Output(value = OutputNames.SECURITY_MODEL_PRICE)
  Result<Double> calculateSecurityModelPrice(Environment env, InterestRateFutureTrade irFutureTrade);
  
  /**
   * Calculates the ir curve sensitivity w.r.t the zero rates - for the ir future trade.
   * @param env the environment that the future contract bucketed delta will be calculated with.
   * @param irFutureTrade the interest rate future trade to calculate the bucketed delta for.
   * @return result containing the zero rate sensitivities sensitivities.
   */
  @Output(value = OutputNames.BUCKETED_ZERO_DELTA)
  Result<BucketedCurveSensitivities> calculateBucketedZeroIRDelta(Environment env, InterestRateFutureTrade irFutureTrade);
  
  
}
