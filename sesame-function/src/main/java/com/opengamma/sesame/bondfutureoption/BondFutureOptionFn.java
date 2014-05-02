/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.trade.BondFutureOptionTrade;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Bond future option results.
 */
public interface BondFutureOptionFn {

  /**
   * Calculates the present value of the bond future option.
   * @param env the environment, not null.
   * @param trade the bond future option trade, not null.
   * @return the present value of the bond future option.
   */
  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(Environment env, BondFutureOptionTrade trade);

  /**
   * Calculate the model price of the bond future option.
   * @param env the environment, not null.
   * @param trade the bond future option trade, not null.
   * @return the model price of the bond future option.
   */
  @Output(value = OutputNames.SECURITY_MODEL_PRICE)
  Result<Double> calculateSecurityModelPrice(Environment env, BondFutureOptionTrade trade);
  
  /**
   * Calculate the delta of the bond future option.
   * @param env the environment, not null.
   * @param trade the bond future option trade, not null.
   * @return the delta of the bond future option.
   */
  @Output(value = OutputNames.DELTA)
  Result<Double> calculateDelta(Environment env, BondFutureOptionTrade trade);
  
  /**
   * Calculate the gamma of the bond future option.
   * @param env the environment, not null.
   * @param trade the bond future option trade, not null.
   * @return the gamma of the bond future option.
   */
  @Output(value = OutputNames.GAMMA)
  Result<Double> calculateGamma(Environment env, BondFutureOptionTrade trade);
  
  /**
   * Calculate the vega of the bond future option.
   * @param env the environment, not null.
   * @param trade the bond future option trade, not null.
   * @return the vega of the bond future option.
   */
  @Output(value = OutputNames.VEGA)
  Result<Double> calculateVega(Environment env, BondFutureOptionTrade trade);
  
  /**
   * Calculate the theta of the bond future option.
   * @param env the environment, not null.
   * @param trade the bond future option trade, not null.
   * @return the theta of the bond future option.
   */
  @Output(value = OutputNames.THETA)
  Result<Double> calculateTheta(Environment env, BondFutureOptionTrade trade);
}
