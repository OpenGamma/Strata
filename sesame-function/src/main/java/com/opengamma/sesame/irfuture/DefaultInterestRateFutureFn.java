/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Default implementation of the {@link InterestRateFutureFn} that uses a specified calculator function to calculate
 * requested values.
 */
public class DefaultInterestRateFutureFn implements InterestRateFutureFn {

  /**
   * The calculator used to provide the OG-Analytics representation of the security and the necessary market data requirements
   * to calculate the requested values.
   */
  private final InterestRateFutureCalculatorFn _interestRateFutureCalculatorFn;
  
  public DefaultInterestRateFutureFn(InterestRateFutureCalculatorFn interestRateFutureCalculatorFn) {
    _interestRateFutureCalculatorFn = interestRateFutureCalculatorFn;
  }
  
  @Override
  public Result<Double> calculateParRate(Environment env, InterestRateFutureSecurity security) {
    Result<InterestRateFutureCalculator> calculatorResult = _interestRateFutureCalculatorFn.generateCalculator(env, security);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculateParRate());
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateFutureSecurity security) {
    Result<InterestRateFutureCalculator> calculatorResult = _interestRateFutureCalculatorFn.generateCalculator(env, security);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePV());
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateFutureSecurity security) {
    Result<InterestRateFutureCalculator> calculatorResult = _interestRateFutureCalculatorFn.generateCalculator(env, security);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePV01());
  }

}
