/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.failure;
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
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Creates a collection of multicurves based on a security and market exposure
 * selector, combining them into a single multicurve.
 */
public class ExposureFunctionsDiscountingMulticurveCombinerFn implements DiscountingMulticurveCombinerFn {

  /**
   * Generates the market exposure selector. In turn this can be used to get
   * an ExposureFunction.
   */
  private final MarketExposureSelectorFn _marketExposureSelectorFn;

  /**
   * Generates a discounting multicurve bundle.
   */
  private final DiscountingMulticurveBundleFn _multicurveBundleProviderFunction;

  public ExposureFunctionsDiscountingMulticurveCombinerFn(MarketExposureSelectorFn marketExposureSelectorFn,
                                                          DiscountingMulticurveBundleFn multicurveBundleProviderFunction) {
    _marketExposureSelectorFn =
        ArgumentChecker.notNull(marketExposureSelectorFn, "marketExposureSelectorFn");
    _multicurveBundleProviderFunction =
        ArgumentChecker.notNull(multicurveBundleProviderFunction, "multicurveBundleProviderFunction");
  }

  @Override
  public Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createMergedMulticurveBundle(
      Environment env, FinancialSecurity security, Result<FXMatrix> fxMatrix) {
    Result<MarketExposureSelector> mesResult = _marketExposureSelectorFn.getMarketExposureSelector();

    if (mesResult.isValueAvailable()) {
      Set<Result<?>> incompleteBundles = new HashSet<>();
      Set<MulticurveProviderDiscount> bundles = new HashSet<>();
      CurveBuildingBlockBundle mergedJacobianBundle = new CurveBuildingBlockBundle();

      MarketExposureSelector selector = mesResult.getValue();
      Set<CurveConstructionConfiguration> curveConfigs = selector.determineCurveConfigurationsForSecurity(security);
      for (CurveConstructionConfiguration curveConfig : curveConfigs) {
        Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundle =
            _multicurveBundleProviderFunction.generateBundle(env, curveConfig);
        if (bundle.isValueAvailable()) {
          Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result = bundle.getValue();
          bundles.add(result.getFirst());
          mergedJacobianBundle.addAll(result.getSecond());
        } else {
          incompleteBundles.add(bundle);
        }
      }

      if (!curveConfigs.isEmpty() && incompleteBundles.isEmpty() && fxMatrix.isValueAvailable()) {
        return success(Pairs.of(mergeBundlesAndMatrix(bundles, fxMatrix.getValue()), mergedJacobianBundle));
      } else if (curveConfigs.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "No matching curves found for security: {}", security);
      } else if (!incompleteBundles.isEmpty()) {
        return propagateFailures(incompleteBundles);
      } else {
        return fxMatrix.propagateFailure();
      }
    } else {
      return mesResult.propagateFailure();
    }
  }

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
