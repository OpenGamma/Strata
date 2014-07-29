/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Default implementation for the bond function.
 */
public class DefaultBondFn implements BondFn {

  private final BondCalculatorFactory _bondCalculatorFactory;

  /**
  * Create the function.
  *
  * @param bondCalculatorFactory function to generate the calculator for the security
  */
  public DefaultBondFn(BondCalculatorFactory bondCalculatorFactory) {
    _bondCalculatorFactory = ArgumentChecker.notNull(bondCalculatorFactory, "bondCalculatorFactory");
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePresentValueFromCurves(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePresentValueFromCurves();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePresentValueFromClean(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePresentValueFromClean();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePresentValueFromYield(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePresentValueFromYield();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<BucketedCurveSensitivities> calculateBucketedPV01(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateBucketedPV01();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePV01();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<Double> calculateZSpread(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateZSpread();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<Double> calculateMarketCleanPrice(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateMarketCleanPrice();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<Double> calculateYieldToMaturity(Environment env, BondTrade bondTrade) {
    Result<BondCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculateYieldToMaturity();
    }
    return Result.failure(calculatorResult);
  }
}
