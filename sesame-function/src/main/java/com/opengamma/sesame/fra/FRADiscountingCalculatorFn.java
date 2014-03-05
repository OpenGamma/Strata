/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.util.tuple.Pair;

public class FRADiscountingCalculatorFn implements FRACalculatorFn {

  /**
   * Factory for creating a calculator for FRA securities.
   */
  private final FRACalculatorFactory _factory;

  /**
   * Generates a combined multicurve bundle suitable for use with a particular security.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;

  public FRADiscountingCalculatorFn(FRACalculatorFactory factory,
                                    DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn) {
    _factory = ArgumentChecker.notNull(factory, "factory");
    _discountingMulticurveCombinerFn =
        ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
  }

  @Override
  public Result<FRACalculator> generateCalculator(final Environment env, final FRASecurity security) {
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult = createBundle(env, security);

    if (!bundleResult.isValueAvailable()) {
      return ResultGenerator.propagateFailure(bundleResult);
    }
    return ResultGenerator.success(_factory.createCalculator(env, security, bundleResult.getValue().getFirst()));
  }

  private Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createBundle(Environment env,
                                                                                          FRASecurity security) {
    return _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, success(new FXMatrix()));
  }
}
