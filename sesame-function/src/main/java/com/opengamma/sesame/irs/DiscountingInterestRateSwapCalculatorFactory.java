/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.InterestRateSwapSecurityConverter;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Factory class for creating a calculator for a discounting swap.
 */
public class DiscountingInterestRateSwapCalculatorFactory implements IInterestRateSwapCalculatorFactory {

  /**
   * Converter for a Swap
   */
  private final InterestRateSwapSecurityConverter _swapConverter;
  /**
   * Function used to generate a combined multicurve bundle suitable
   * for use with a particular security.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;

  public DiscountingInterestRateSwapCalculatorFactory(InterestRateSwapSecurityConverter swapConverter,
                                                      DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn) {
    _swapConverter = ArgumentChecker.notNull(swapConverter, "swapConverter");
    _discountingMulticurveCombinerFn = ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
  }

  public DiscountingInterestRateSwapCalculator createCalculator(Environment env,
                                                     InterestRateSwapSecurity security,
                                                     MulticurveProviderDiscount bundle) {
    return new DiscountingInterestRateSwapCalculator(security, bundle, _swapConverter, env.getValuationTime());
  }

  @Override
  public Result<InterestRateSwapCalculator> createCalculator(Environment env, InterestRateSwapSecurity security) {
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
        _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, Result.success(new FXMatrix()));

    if (bundleResult.isSuccess()) {
      InterestRateSwapCalculator calculator = new DiscountingInterestRateSwapCalculator(security,
                                                                                        bundleResult.getValue().getFirst(),
                                                                                        _swapConverter,
                                                                                        env.getValuationTime());
      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult);
    }
  }
}
