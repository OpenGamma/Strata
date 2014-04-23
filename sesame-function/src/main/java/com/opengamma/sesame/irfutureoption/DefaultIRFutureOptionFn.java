/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.IRFutureOptionTrade;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Default implementation of the {@link IRFutureOptionFn} that uses a specified calculator function to calculate requested
 * values.
 */
public class DefaultIRFutureOptionFn implements IRFutureOptionFn {
  
  private final IRFutureOptionCalculatorFactory _irFutureOptionCalculatorFactory;
  
  public DefaultIRFutureOptionFn(IRFutureOptionCalculatorFactory irFutureOptionCalculatorFactory) {
    _irFutureOptionCalculatorFactory = irFutureOptionCalculatorFactory;
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (!(calculatorResult.isSuccess())) {
      return Result.failure(calculatorResult);
    }
    return calculatorResult.getValue().calculatePV();
  }

  @Override
  public Result<MultipleCurrencyMulticurveSensitivity> calculatePV01(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (!(calculatorResult.isSuccess())) {
      return Result.failure(calculatorResult);
    }
    return calculatorResult.getValue().calculatePV01();
  }
}
