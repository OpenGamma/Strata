/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.cashflows.SwapLegCashFlows;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculate discounting PV and par rate for a Swap.
 */
public class DiscountingInterestRateSwapFn implements InterestRateSwapFn {

  private final InterestRateSwapCalculatorFactory _interestRateSwapCalculatorFactory;

  /**
   * Create the function.
   *
   * @param interestRateSwapCalculatorFactory function to generate the calculator for the security
   */
  public DiscountingInterestRateSwapFn(InterestRateSwapCalculatorFactory interestRateSwapCalculatorFactory) {
    _interestRateSwapCalculatorFactory = interestRateSwapCalculatorFactory;

  }

  @Override
  public Result<Double> calculateParRate(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculateRate().getValue());
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePV().getValue());
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePV01().getValue());
  }

  @Override
  public Result<SwapLegCashFlows> calculateReceiveLegCashFlows(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculateReceiveLegCashFlows().getValue());
  }

  @Override
  public Result<SwapLegCashFlows> calculatePayLegCashFlows(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return Result.success(calculatorResult.getValue().calculatePayLegCashFlows().getValue());
  }

}
