/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfuture;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Default implementation of the {@link InterestRateFutureCalculatorFn}.
 */
public class InterestRateFutureDiscountingCalculatorFn implements InterestRateFutureCalculatorFn {
  
  /**
   * Factory to create a calculator for interest rate future securities.
   */
  private final InterestRateFutureCalculatorFactory _factory;
  
  /**
   * Generates a multicurve bundle for use with the interest rate future calculator.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;
  
  private final HistoricalTimeSeriesFn _htsFn;
  
  public InterestRateFutureDiscountingCalculatorFn(InterestRateFutureCalculatorFactory factory,
                                                   DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn,
                                                   HistoricalTimeSeriesFn htsFn) {
    _factory = ArgumentChecker.notNull(factory, "factory");
    _discountingMulticurveCombinerFn =
        ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
  }

  @Override
  public Result<InterestRateFutureCalculator> generateCalculator(Environment env, InterestRateFutureSecurity security) {
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
        _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, Result.success(new FXMatrix()));

    Result<HistoricalTimeSeriesBundle> fixingsResult = _htsFn.getFixingsForSecurity(env, security);
    
    if (Result.allSuccessful(bundleResult, fixingsResult)) {

      
      return Result.success(_factory.createCalculator(env, security, bundleResult.getValue().getFirst(), fixingsResult.getValue()));
    } else {
      return Result.failure(bundleResult, fixingsResult);
    }
  }

}
