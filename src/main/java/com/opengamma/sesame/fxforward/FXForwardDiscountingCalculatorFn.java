/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.ResultGenerator.map;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.util.tuple.Pair;

public class FXForwardDiscountingCalculatorFn implements FXForwardCalculatorFn {

  /**
   * Factory for creating a calculator for FX Forward securities.
   */
  private final FXForwardCalculatorFactory _factory;

  /**
   * Function to generate an FX Matrix based on the currencies of an FX Forward.
   */
  private final FXMatrixFn _fxMatrixProvider;

  /**
   * Generates a combined multicurve bundle suitable for use with a particular security.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;

  public FXForwardDiscountingCalculatorFn(FXForwardCalculatorFactory factory,
                                          FXMatrixFn fxMatrixProvider,
                                          DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn) {
    _factory = ArgumentChecker.notNull(factory, "factory");
    _fxMatrixProvider = ArgumentChecker.notNull(fxMatrixProvider, "fxMatrixProvider");
    _discountingMulticurveCombinerFn =
        ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
  }

  @Override
  public Result<FXForwardCalculator> generateCalculator(final FXForwardSecurity security) {

    return map(createBundle(security),
               new ResultGenerator.ResultMapper<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>, FXForwardCalculator>() {
      @Override
      public Result<FXForwardCalculator> map(Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result) {
        return success(_factory.createCalculator(security, result.getFirst(), result.getSecond()));
      }
    });
  }

  private Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createBundle(FXForwardSecurity security) {

    // get currencies from security, probably should use visitor/utils
    Set<Currency> currencies = ImmutableSet.of(security.getPayCurrency(), security.getReceiveCurrency());

    // Even if we can't get a matrix we want to get as far as we can to
    // ensure market data population, so ignore the result for now
    Result<FXMatrix> fxmResult = _fxMatrixProvider.getFXMatrix(currencies);
    return _discountingMulticurveCombinerFn.createMergedMulticurveBundle(security, fxmResult);
  }
}
