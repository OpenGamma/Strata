/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Default implementation for the bond function.
 */
public class DefaultBondFn implements BondFn {

  private final BondCalculatorFactory _bondCalculatorFactory;

  public DefaultBondFn(BondCalculatorFactory bondCalculatorFactory) {
    _bondCalculatorFactory = ArgumentChecker.notNull(bondCalculatorFactory, "bondCalculatorFactory");
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, BondTrade bondTrade) {
    Result<BondDiscountingCalculator> calculatorResult = _bondCalculatorFactory.createCalculator(env, bondTrade);
    if (calculatorResult.isSuccess()) {
      return calculatorResult.getValue().calculatePV();
    }
    return Result.failure(calculatorResult);
  }
}
