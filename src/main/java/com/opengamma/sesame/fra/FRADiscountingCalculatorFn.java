/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.map;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.propagateFailures;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.MarketExposureSelector;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

public class FRADiscountingCalculatorFn implements FRACalculatorFn {

  private final FRACalculatorFactory _factory;

  /**
   * Generates the market exposure selector. In turn this can be used to get
   * an ExposureFunction.
   */
  private final MarketExposureSelectorFn _marketExposureSelectorFn;

  /**
   * Generates a discounting multicurve bundle.
   */
  private final DiscountingMulticurveBundleFn _multicurveBundleProviderFunction;

  public FRADiscountingCalculatorFn(FRACalculatorFactory factory,
                                    MarketExposureSelectorFn marketExposureSelectorFn,
                                    DiscountingMulticurveBundleFn multicurveBundleProviderFunction) {
    _factory = ArgumentChecker.notNull(factory, "factory");
    _marketExposureSelectorFn =
        ArgumentChecker.notNull(marketExposureSelectorFn, "marketExposureSelectorFn");
    _multicurveBundleProviderFunction =
        ArgumentChecker.notNull(multicurveBundleProviderFunction, "multicurveBundleProviderFunction");
  }


  @Override
  public Result<FRACalculator> generateCalculator(final FRASecurity security) {

    return map(createBundle(security),
               new ResultGenerator.ResultMapper<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>, FRACalculator>() {
      @Override
      public Result<FRACalculator> map(Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result) {
        return success(_factory.createCalculator(security, result.getFirst(), result.getSecond()));
      }
    });
  }

  private Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createBundle(FRASecurity security) {

    Result<MarketExposureSelector> mesResult = _marketExposureSelectorFn.getMarketExposureSelector();

    /*
        if (mesResult.isValueAvailable()) {
      Set<Result<?>> incompleteBundles = new HashSet<>();
      Set<MulticurveProviderDiscount> bundles = new HashSet<>();
      MarketExposureSelector selector = mesResult.getValue();
      Set<CurveConstructionConfiguration> curveConfigs = selector.determineCurveConfigurationsForSecurity(security);
      for (CurveConstructionConfiguration curveConfig : curveConfigs) {
        Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundle =
            _multicurveBundleProviderFunction.generateBundle(curveConfig);
        if (bundle.isValueAvailable()) {
          Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result = bundle.getValue();
          bundles.add(result.getFirst());
        } else {
          incompleteBundles.add(bundle);
        }
      }

      if (!curveConfigs.isEmpty() && incompleteBundles.isEmpty()) {
        return success(mergeBundlesAndMatrix(bundles, new FXMatrix()));
      } else if (curveConfigs.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "No matching curves found for security: {}", security);
      } else {
        return propagateFailures(incompleteBundles);
      }
    } else {
      return propagateFailure(mesResult);
    }
     */

    if (mesResult.isValueAvailable()) {
      Set<Result<?>> incompleteBundles = new HashSet<>();
      Set<MulticurveProviderDiscount> bundles = new HashSet<>();
      CurveBuildingBlockBundle mergedJacobianBundle = new CurveBuildingBlockBundle();

      MarketExposureSelector selector = mesResult.getValue();
      Set<CurveConstructionConfiguration> curveConfigs = selector.determineCurveConfigurationsForSecurity(security);
      for (CurveConstructionConfiguration curveConfig : curveConfigs) {
        Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundle =
            _multicurveBundleProviderFunction.generateBundle(curveConfig);
        if (bundle.isValueAvailable()) {
          Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result = bundle.getValue();
          bundles.add(result.getFirst());
          mergedJacobianBundle.addAll(result.getSecond());
        } else {
          incompleteBundles.add(bundle);
        }
      }

      if (!curveConfigs.isEmpty() && incompleteBundles.isEmpty()) {
        return success(Pairs.of(mergeBundlesAndMatrix(bundles, new FXMatrix()), mergedJacobianBundle));
      } else if (curveConfigs.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "No matching curves found for security: {}", security);
      } else {
        return propagateFailures(incompleteBundles);
      }
    } else {
      return propagateFailure(mesResult);
    }

  }

  //TODO reference the FXForwardDiscountingCalculatorFn
  private MulticurveProviderDiscount mergeBundlesAndMatrix(Collection<MulticurveProviderDiscount> providers,
                                                           FXMatrix fxMatrix) {
    return providers.size() > 1 ?
        ProviderUtils.mergeDiscountingProviders(mergeBundles(providers), fxMatrix) :
        providers.iterator().next();
  }

  private MulticurveProviderDiscount mergeBundles(Collection<MulticurveProviderDiscount> providers) {
    return ProviderUtils.mergeDiscountingProviders(providers);
  }

}
