/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;
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
  @Output(OutputNames.SECURITY_MODEL_PRICE)
  Result<Double> calculateSecurityModelPrice(Environment env, DeliverableSwapFutureTrade delivSwapFutureTrade);
  
  /**
   * Calculates the PV01 of the deliverable swap future contract.
   * @param env the environment that the PV01 will be calculated with.
   * @param delivSwapFutureTrade the swap future trade to calculate the, per curve, PV01 for.
   * @return result containing the PV01 for each curve.
   */
  @Output(OutputNames.PV01)
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, DeliverableSwapFutureTrade delivSwapFutureTrade);

  /**
   * Calculates the ir curve sensitivity w.r.t the zero rates - for the deliverable swap future trade.
   * @param env the environment that the future contract bucketed delta will be calculated with.
   * @param delivSwapFutureTrade the deliverable swap future trade to calculate the bucketed delta for.
   * @return result containing the bucketed zero rate sensitivities if successfully created, a failure result otherwise..
   */
  @Output(OutputNames.BUCKETED_ZERO_DELTA)
  Result<BucketedCurveSensitivities> calculateBucketedZeroIRDelta(Environment env, DeliverableSwapFutureTrade delivSwapFutureTrade);
  
}