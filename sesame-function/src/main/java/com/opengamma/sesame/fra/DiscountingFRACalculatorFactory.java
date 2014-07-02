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
public class DiscountingFRACalculatorFactory implements FRACalculatorFactory {

  /**
   * Converter for a FRA.
   */
  private final FRASecurityConverter _fraConverter;

  /**
   * Function used to generate a combined multicurve bundle suitable
   * for use with a particular security.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;

  /**
   * Creates the factory.
   *
   * @param discountingMulticurveCombinerFn function for creating multicurve bundles, not null
   * @param fraConverter converter for transforming a fra into its InstrumentDefinition form, not null
   */
  public DiscountingFRACalculatorFactory(FRASecurityConverter fraConverter,
                                         DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn) {
    _fraConverter = ArgumentChecker.notNull(fraConverter, "fraConverter");
    _discountingMulticurveCombinerFn = ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
  }

  @Override
  public Result<FRACalculator> createCalculator(Environment env, FRASecurity security) {

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
               _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, new FXMatrix());

    if (bundleResult.isSuccess()) {
      FRACalculator calculator = new DiscountingFRACalculator(security,
                                                               bundleResult.getValue().getFirst(),
                                                               _fraConverter,
                                                               env.getValuationTime());
      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult);
    }
  }
}
