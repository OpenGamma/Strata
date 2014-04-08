/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.InterestRateFutureTrade;
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
  private final InterestRateFutureCalculatorFactory _interestRateFutureCalculatorFactory;
  
  public DefaultInterestRateFutureFn(InterestRateFutureCalculatorFactory interestRateFutureCalculatorFactory) {
    _interestRateFutureCalculatorFactory = interestRateFutureCalculatorFactory;
  }
  
  @Override
  public Result<Double> calculateParRate(Environment env, InterestRateFutureTrade irFutureTrade) {
    Result<InterestRateFutureCalculator> calculatorResult = _interestRateFutureCalculatorFactory.createCalculator(env, irFutureTrade);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculateParRate());
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateFutureTrade irFutureTrade) {
    Result<InterestRateFutureCalculator> calculatorResult = _interestRateFutureCalculatorFactory.createCalculator(env, irFutureTrade);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePV());
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateFutureTrade irFutureTrade) {
    Result<InterestRateFutureCalculator> calculatorResult = _interestRateFutureCalculatorFactory.createCalculator(env, irFutureTrade);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePV01());
  }

}
