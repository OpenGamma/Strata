/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;


/**
 * Calculate discounting PV and par rate for a FRA.
 */
public class DiscountingFRAFn implements FRAFn {

  private final IFRACalculatorFactory _fraCalculatorFactory;

  /**
   * Create the function.
   *
   * @param fraCalculatorFactory function to generate the calculator for the security
   */
  public DiscountingFRAFn(IFRACalculatorFactory fraCalculatorFactory) {
    _fraCalculatorFactory = fraCalculatorFactory;
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, FRASecurity security) {
    Result<IFRACalculator> calculatorResult = _fraCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePV().getValue());
  }

  @Override
  public Result<Double> calculateParRate(Environment env, FRASecurity security) {
    Result<IFRACalculator> calculatorResult = _fraCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculateRate().getValue());
  }
}
