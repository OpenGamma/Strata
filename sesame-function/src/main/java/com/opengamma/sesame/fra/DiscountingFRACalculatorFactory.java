/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FRASecurityConverter;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Factory class for creating a calculator for a discounting FRA.
 */
public class DiscountingFRACalculatorFactory implements IFRACalculatorFactory {

  /**
   * Converter for a FRA.
   */
  private final FRASecurityConverter _fraConverter;

  /**
   * Function used to generate a combined multicurve bundle suitable
   * for use with a particular security.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;

  public DiscountingFRACalculatorFactory(FRASecurityConverter fraConverter,
                                         DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn) {
    _fraConverter = ArgumentChecker.notNull(fraConverter, "fraConverter");
    _discountingMulticurveCombinerFn = ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
  }

  public DiscountingFRACalculator createCalculator(Environment env, FRASecurity security, MulticurveProviderDiscount bundle) {
    return new DiscountingFRACalculator(security, bundle, _fraConverter, env.getValuationTime());
  }

  @Override
  public Result<IFRACalculator> createCalculator(Environment env, FRASecurity security) {

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
               _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, Result.success(new FXMatrix()));

    if (bundleResult.isSuccess()) {
      IFRACalculator calculator = new DiscountingFRACalculator(security,
                                                               bundleResult.getValue().getFirst(),
                                                               _fraConverter,
                                                               env.getValuationTime());
      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult);
    }
  }
}
