/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultMapper;


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
  public Result<MultipleCurrencyAmount> calculatePV(FRASecurity security) {

    return calculate(security, new ResultMapper<FRACalculator, MultipleCurrencyAmount>() {
      @Override
      public Result<MultipleCurrencyAmount> map(FRACalculator result) {
        return success(result.calculatePV());
      }
    });
  }

  @Override
  public Result<Double> calculateParRate(FRASecurity security) {

    return calculate(security, new ResultMapper<FRACalculator, Double>() {
      @Override
      public Result<Double> map(FRACalculator result) {
        return success(result.calculateRate());
      }
    });
  }

  private <T> Result<T> calculate(FRASecurity security, ResultMapper<FRACalculator, T> mapper) {
    Result<FRACalculator> calculator = _FRACalculatorFn.generateCalculator(security);
    return calculator.map(mapper);
  }
}
