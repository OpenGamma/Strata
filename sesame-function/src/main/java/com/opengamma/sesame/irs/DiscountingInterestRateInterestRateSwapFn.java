/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import static com.opengamma.util.result.ResultGenerator.map;
import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.financial.security.swap.SwapSecurity;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;

/**
 * Calculate discounting PV and par rate for a Swap.
 */
public class DiscountingInterestRateInterestRateSwapFn implements InterestRateSwapFn {

  private final InterestRateSwapCalculatorFn _interestRateSwapCalculatorFn;

  /**
   * Create the function.
   *
   * @param interestRateSwapCalculatorFn function to generate the calculator for the security
   */
  public DiscountingInterestRateInterestRateSwapFn(InterestRateSwapCalculatorFn interestRateSwapCalculatorFn) {
    _interestRateSwapCalculatorFn = interestRateSwapCalculatorFn;

  }

  @Override
  public Result<Double> calculateParRate(InterestRateSwapSecurity security) {
    return calculate(security, new ResultGenerator.ResultMapper<InterestRateSwapCalculator, Double>() {
      @Override
      public Result<Double> map(InterestRateSwapCalculator result) {
        return success(result.calculateRate());
      }
    });
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(InterestRateSwapSecurity security) {
    return calculate(security, new ResultGenerator.ResultMapper<InterestRateSwapCalculator, MultipleCurrencyAmount>() {
      @Override
      public Result<MultipleCurrencyAmount> map(InterestRateSwapCalculator result) {
        return success(result.calculatePV());
      }
    });
  }

  private <T> Result<T> calculate(InterestRateSwapSecurity security, ResultGenerator.ResultMapper<InterestRateSwapCalculator, T> mapper) {
    Result<InterestRateSwapCalculator> calculator = _interestRateSwapCalculatorFn.generateCalculator(security);
    return map(calculator, mapper);
  }
}
