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
  
  /**
   * Factory that creates interest rate future option calculators.
   */
  private final IRFutureOptionCalculatorFactory _irFutureOptionCalculatorFactory;
  
  /**
   * Constructs an instance of {@link IRFutureOptionFn} using a calculator created from a specified factory to compute values.
   * @param irFutureOptionCalculatorFactory factory that creates a calculator.
   */
  public DefaultIRFutureOptionFn(IRFutureOptionCalculatorFactory irFutureOptionCalculatorFactory) {
    _irFutureOptionCalculatorFactory = irFutureOptionCalculatorFactory;
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePV();
    } else {
      return Result.failure(calculatorResult);
    }
  }

  @Override
  public Result<MultipleCurrencyMulticurveSensitivity> calculatePV01(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePV01();
    } else {
      return Result.failure(calculatorResult);
    }
  }
  
  @Override
  public Result<Double> calculateModelPrice(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateModelPrice();
    } else {
      return Result.failure(calculatorResult);
    }
  }
  
  @Override
  public Result<Double> calculateDelta(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateDelta();
    } else {
      return Result.failure(calculatorResult);
    }
  }
  
  @Override
  public Result<Double> calculateGamma(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateGamma();
    } else {
      return Result.failure(calculatorResult);
    }
  }
  
  @Override
  public Result<Double> calculateVega(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateVega();
    } else {
      return Result.failure(calculatorResult);
    }
  }
  
  @Override
  public Result<Double> calculateTheta(Environment env, IRFutureOptionTrade trade) {
    Result<IRFutureOptionCalculator> calculatorResult = _irFutureOptionCalculatorFactory.createCalculator(env, trade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateTheta();
    } else {
      return Result.failure(calculatorResult);
    }
  }
}
