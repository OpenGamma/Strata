/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.InterestRateSwapSecurityConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Factory class for creating a calculator for a discounting swap.
 */
public class DiscountingInterestRateSwapCalculatorFactory implements InterestRateSwapCalculatorFactory {

  /**
   * Converter for a Swap
   */
  private final InterestRateSwapSecurityConverter _swapConverter;
  /**
   * Function used to generate a combined multicurve bundle suitable
   * for use with a particular security.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;
  /**
   * Definition to derivative converter
   */
  private final FixedIncomeConverterDataProvider _fixedIncomeConverterDataProvider;

  /**
   * HTS function for fixings
   */
  private final HistoricalTimeSeriesFn _htsFn;

  /**
   * Curve definition function
   */
  private final CurveDefinitionFn _curveDefinitionFn;

  /**
   * Creates the factory.
   *
   * @param discountingMulticurveCombinerFn function for creating multicurve bundles, not null
   * @param swapConverter converter for transforming a swap into its InstrumentDefinition form, not null
   * @param definitionConverter converter for transforming a definition into a derivative, not null.
   * @param htsFn hts function for providing fixing timeseries, not null.
   */
  public DiscountingInterestRateSwapCalculatorFactory(InterestRateSwapSecurityConverter swapConverter, FixedIncomeConverterDataProvider definitionConverter,
      DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn, HistoricalTimeSeriesFn htsFn,
      CurveDefinitionFn curveDefinitionFn) {
    _swapConverter = ArgumentChecker.notNull(swapConverter, "swapConverter");
    _fixedIncomeConverterDataProvider = ArgumentChecker.notNull(definitionConverter, "definitionConverter");
    _discountingMulticurveCombinerFn = ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
    _curveDefinitionFn = curveDefinitionFn;
  }

  @Override
  public Result<InterestRateSwapCalculator> createCalculator(Environment env, InterestRateSwapSecurity security) {
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
        _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, Result.success(new FXMatrix()));

    final Result<HistoricalTimeSeriesBundle> fixings = _htsFn.getFixingsForSecurity(env, security);

    if (Result.allSuccessful(bundleResult, fixings)) {
      InterestRateSwapCalculator calculator = new DiscountingInterestRateSwapCalculator(security, bundleResult.getValue().getFirst(),
          bundleResult.getValue().getSecond(), _swapConverter, env.getValuationTime(), _fixedIncomeConverterDataProvider, fixings.getValue(), _curveDefinitionFn);
      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult, fixings);
    }
  }
}
