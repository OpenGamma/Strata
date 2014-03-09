/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.util.tuple.Pair;

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
  public Result<Double> calculateParRate(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFn.generateCalculator(env, security);

    if (!calculatorResult.isValueAvailable()) {
      return calculatorResult.propagateFailure();
    }
    return ResultGenerator.success(calculatorResult.getValue().calculateRate());
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFn.generateCalculator(env, security);

    if (!calculatorResult.isValueAvailable()) {
      return calculatorResult.propagateFailure();
    }
    return ResultGenerator.success(calculatorResult.getValue().calculatePV());
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFn.generateCalculator(env, security);

    if (!calculatorResult.isValueAvailable()) {
      return calculatorResult.propagateFailure();
    }
    return ResultGenerator.success(calculatorResult.getValue().calculatePV01());
  }
}
