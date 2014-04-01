/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculate PV and par rate of an interest rate future.
 */
public interface InterestRateFutureFn {

  /**
   * Calculate the par rate of the interest rate future.
   * 
   * @param env the environment that the par rate will be calculated with.
   * @param security the interest rate future to calculate the par rate for.
   * @return result containing the par rate if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PAR_RATE)
  Result<Double> calculateParRate(Environment env, InterestRateFutureSecurity security);

  /**
   * Calculate the present value of the interest rate future.
   *
   * @param env the environment that the PV will be calculated with.
   * @param security the interest rate future to calculate the PV for.
   * @return result containing the present value if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateFutureSecurity security);
  
  /**
   * Calculate the PV01 for an interest rate future security.
   * 
   * @param env the environment that the PV01 will be calculated with.
   * @param security the interest rate future to calculate the PV for.
   * @return result containing the PV01 if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PV01)
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateFutureSecurity security);
}
