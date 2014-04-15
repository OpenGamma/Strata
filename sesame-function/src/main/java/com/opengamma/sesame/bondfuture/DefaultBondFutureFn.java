/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.BondFutureTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Default implementation for the bond future function.
 */
public class DefaultBondFutureFn implements BondFutureFn {
  
  private final BondFutureCalculatorFactory _bondFutureCalculatorFactory;

  public DefaultBondFutureFn(BondFutureCalculatorFactory bondFutureCalculatorFactory) {
    _bondFutureCalculatorFactory = ArgumentChecker.notNull(bondFutureCalculatorFactory, "bondFutureCalculatorFactory");
  }
  
  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, BondFutureTrade bondFutureTrade) {
    Result<BondFutureDiscountingCalculator> calculatorResult = _bondFutureCalculatorFactory.createCalculator(env, bondFutureTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePV();
    }
    return Result.failure(calculatorResult);
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, BondFutureTrade bondFutureTrade) {
    Result<BondFutureDiscountingCalculator> calculatorResult = _bondFutureCalculatorFactory.createCalculator(env, bondFutureTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePV01();
    }
    return Result.failure(calculatorResult);
  }
}
