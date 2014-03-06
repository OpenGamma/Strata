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
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultMapper;
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
  public Result<FRACalculator> generateCalculator(final FRASecurity security) {

    return createBundle(security).map(
        new ResultMapper<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>, FRACalculator>() {
          @Override
          public Result<FRACalculator> map(Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result) {
            return success(_factory.createCalculator(security, result.getFirst(), result.getSecond()));
          }
        }
    );
  }

  private Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createBundle(FRASecurity security) {
    return _discountingMulticurveCombinerFn.createMergedMulticurveBundle(security, success(new FXMatrix()));
  }
}
