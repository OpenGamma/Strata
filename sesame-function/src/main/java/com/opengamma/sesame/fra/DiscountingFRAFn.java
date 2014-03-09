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
import com.opengamma.util.result.ResultGenerator;


/**
 * Calculate discounting PV and par rate for a FRA.
 */
public class DiscountingFRAFn implements FRAFn {

  private final FRACalculatorFn _FRACalculatorFn;

  /**
   * Create the function.
   *
   * @param FRACalculatorFn function to generate the calculator for the security
   */
  public DiscountingFRAFn(FRACalculatorFn FRACalculatorFn) {
    _FRACalculatorFn = FRACalculatorFn;
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, FRASecurity security) {
    Result<FRACalculator> calculatorResult = _FRACalculatorFn.generateCalculator(env, security);

    if (!calculatorResult.isValueAvailable()) {
      return calculatorResult.propagateFailure();
    }
    return ResultGenerator.success(calculatorResult.getValue().calculatePV());
  }

  @Override
  public Result<Double> calculateParRate(Environment env, FRASecurity security) {
    Result<FRACalculator> calculatorResult = _FRACalculatorFn.generateCalculator(env, security);

    if (!calculatorResult.isValueAvailable()) {
      return calculatorResult.propagateFailure();
    }
    return ResultGenerator.success(calculatorResult.getValue().calculateRate());
  }
}
