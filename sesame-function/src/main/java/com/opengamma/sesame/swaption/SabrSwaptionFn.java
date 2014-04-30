/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import com.google.common.base.Function;
import com.opengamma.analytics.financial.interestrate.PresentValueSABRSensitivityDataBundle;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculate analytics values for a swaption using SABR model.
 */
public class SabrSwaptionFn implements SwaptionFn {

  /**
   * Function which will generate a calculator for a swaption security, not null.
   */
  private final SwaptionCalculatorFactory _swaptionCalculatorFactory;

  /**
   * Performs analytics calculations for swaption securities.
   *
   * @param swaptionCalculatorFactory function which will generate a calculator
   * for a swaption security, not null
   */
  public SabrSwaptionFn(SwaptionCalculatorFactory swaptionCalculatorFactory) {
    _swaptionCalculatorFactory = ArgumentChecker.notNull(swaptionCalculatorFactory, "swaptionCalculatorFactory");
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, SwaptionSecurity security) {

    return calculate(env, security, new Function<SwaptionCalculator, Result<MultipleCurrencyAmount>>() {
      @Override
      public Result<MultipleCurrencyAmount> apply(SwaptionCalculator calculator) {
        return calculator.calculatePV();
      }
    });
  }

  @Override
  public Result<Double> calculateImpliedVolatility(Environment env, SwaptionSecurity security) {

    return calculate(env, security, new Function<SwaptionCalculator, Result<Double>>() {
      @Override
      public Result<Double> apply(SwaptionCalculator calculator) {
        return calculator.calculateImpliedVolatility();
      }
    });
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, SwaptionSecurity security) {
    return calculate(env, security, new Function<SwaptionCalculator, Result<ReferenceAmount<Pair<String, Currency>>>>() {
      @Override
      public Result<ReferenceAmount<Pair<String, Currency>>> apply(SwaptionCalculator calculator) {
        return calculator.calculatePV01();
      }
    });
  }

  @Override
  public Result<MultipleCurrencyParameterSensitivity> calculateBucketedPV01(Environment env, SwaptionSecurity security) {

    return calculate(env, security, new Function<SwaptionCalculator, Result<MultipleCurrencyParameterSensitivity>>() {
      @Override
      public Result<MultipleCurrencyParameterSensitivity> apply(SwaptionCalculator calculator) {
        return calculator.calculateBucketedPV01();
      }
    });
  }

  @Override
  public Result<PresentValueSABRSensitivityDataBundle> calculateBucketedSABRRisk(Environment env,
                                                                                 SwaptionSecurity security) {

    return calculate(env, security, new Function<SwaptionCalculator, Result<PresentValueSABRSensitivityDataBundle>>() {
      @Override
      public Result<PresentValueSABRSensitivityDataBundle> apply(SwaptionCalculator calculator) {
        return calculator.calculateBucketedSABRRisk();
      }
    });
  }

  /**
   * Create the calculator instance required for supplied security and then execute
   * the requested calculation.
   *
   * @param env the environment being used
   * @param security the security to perform the calculation for
   * @param calculation the calculation to be executed
   * @param <T> the return type of the calculation
   * @return the result of the calculation (wrapped in a Result object)
   */
  private <T> Result<T> calculate(Environment env, SwaptionSecurity security,
                                  Function<SwaptionCalculator, Result<T>> calculation) {

    Result<SwaptionCalculator> calculator = _swaptionCalculatorFactory.createCalculator(env, security);
    return calculator.flatMap(calculation);
  }
}
