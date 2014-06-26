/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.trade.IRFutureOptionTrade;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Interest rate future option results.
 */
public interface IRFutureOptionFn {

  /**
   * Calculates the present value of the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return the present value of the interest rate future option.
   */
  @Output(OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(Environment env, IRFutureOptionTrade trade);
  
  /**
   * Calculates the PV01 of the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return the PV01 of the interest rate future option.
   */
  @Output(OutputNames.PV01)
  Result<MultipleCurrencyMulticurveSensitivity> calculatePV01(Environment env, IRFutureOptionTrade trade);
  
  /**
   * Calculates the model price of the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return the model price of the interest rate future option.
   */
  @Output(OutputNames.SECURITY_MODEL_PRICE)
  Result<Double> calculateModelPrice(Environment env, IRFutureOptionTrade trade);
  
  /**
   * Calculates the delta of the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return the delta of the interest rate future option.
   */
  @Output(OutputNames.DELTA)
  Result<Double> calculateDelta(Environment env, IRFutureOptionTrade trade);
  
  /**
   * Calculates the gamma of the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return the gamma of the interest rate future option.
   */
  @Output(OutputNames.GAMMA)
  Result<Double> calculateGamma(Environment env, IRFutureOptionTrade trade);
  
  /**
   * Calculates the vega of the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return the vega of the interest rate future option.
   */
  @Output(OutputNames.VEGA)
  Result<Double> calculateVega(Environment env, IRFutureOptionTrade trade);
  
  /**
   * Calculates the theta of the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return the theta of the interest rate future option.
   */
  @Output(OutputNames.THETA)
  Result<Double> calculateTheta(Environment env, IRFutureOptionTrade trade);
  
  /**
   * Calculates the sensitivity w.r.t the zero rates for the interest rate future option.
   * @param env the environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return result containing the bucketed zero rate sensitivities if successfully created, a failure result otherwise.
   */
  @Output(OutputNames.BUCKETED_ZERO_DELTA)
  Result<BucketedCurveSensitivities> calculateBucketedZeroIRDelta(Environment env, IRFutureOptionTrade trade);
  
}
