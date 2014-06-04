/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.financial.analytics.model.fixedincome.SwapLegCashFlows;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.Environment;
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
    Result<Double> rateResult = calculatorResult.getValue().calculateRate();
    if (!rateResult.isSuccess()) {
      return Result.failure(rateResult);
    }
    return Result.success(rateResult.getValue());
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    Result<MultipleCurrencyAmount> pvResult = calculatorResult.getValue().calculatePV();
    if (!pvResult.isSuccess()) {
      return Result.failure(pvResult);
    }
    return Result.success(pvResult.getValue());
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    Result<ReferenceAmount<Pair<String, Currency>>> pv01Result = calculatorResult.getValue().calculatePV01();
    if (!pv01Result.isSuccess()) {
      return Result.failure(pv01Result);
    }
    return Result.success(pv01Result.getValue());
  }

  @Override
  public Result<BucketedCurveSensitivities> calculateBucketedPV01(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return calculatorResult.getValue().calculateBucketedPV01();
  }

  @Override
  public Result<SwapLegCashFlows> calculateReceiveLegCashFlows(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    Result<SwapLegCashFlows> legResult = calculatorResult.getValue().calculateReceiveLegCashFlows();
    if (!legResult.isSuccess()) {
      return Result.failure(legResult);
    }
    return Result.success(legResult.getValue());
  }

  @Override
  public Result<SwapLegCashFlows> calculatePayLegCashFlows(Environment env, InterestRateSwapSecurity security) {
    Result<InterestRateSwapCalculator> calculatorResult = _interestRateSwapCalculatorFactory.createCalculator(env, security);

    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    Result<SwapLegCashFlows> legResult = calculatorResult.getValue().calculatePayLegCashFlows();
    if (!legResult.isSuccess()) {
      return Result.failure(legResult);
    }
    return Result.success(legResult.getValue());
  }

}
