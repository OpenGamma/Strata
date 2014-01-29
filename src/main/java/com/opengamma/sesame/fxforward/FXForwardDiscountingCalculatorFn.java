/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.sesame.MarketExposureSelector;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.Pair;

public class FXForwardDiscountingCalculatorFn implements FXForwardCalculatorFn {

  private final FXForwardCalculatorFactory _factory;

  private final MarketExposureSelectorFn _marketExposureSelectorFn;
  private final FXMatrixFn _fxMatrixProvider;
  private final DiscountingMulticurveBundleFn _multicurveBundleProviderFunction;

  public FXForwardDiscountingCalculatorFn(FXForwardCalculatorFactory factory,
                                          MarketExposureSelectorFn marketExposureSelectorFn,
                                          FXMatrixFn fxMatrixProvider,
                                          DiscountingMulticurveBundleFn multicurveBundleProviderFunction) {
    _factory = factory;
    _marketExposureSelectorFn = marketExposureSelectorFn;
    _fxMatrixProvider = fxMatrixProvider;
    _multicurveBundleProviderFunction = multicurveBundleProviderFunction;
  }

  @Override
  public Result<FXForwardCalculator> generateCalculator(FXForwardSecurity security) {

    // get currencies from security, probably should use visitor/utils
    Set<Currency> currencies = ImmutableSet.of(security.getPayCurrency(), security.getReceiveCurrency());

    // Even if we can't get a matrix we want to get as far as we can to
    // ensure market data population, so ignore the result for now
    Result<FXMatrix> fxmResult = _fxMatrixProvider.getFXMatrix(currencies);

    Result<MarketExposureSelector> mesResult = _marketExposureSelectorFn.getMarketExposureSelector();

    if (mesResult.isValueAvailable()) {

      Set<String> incompleteBundles = new HashSet<>();
      Set<MulticurveProviderDiscount> bundles = new HashSet<>();
      CurveBuildingBlockBundle mergedJacobianBundle = new CurveBuildingBlockBundle();

      MarketExposureSelector selector = mesResult.getValue();
      Set<CurveConstructionConfiguration> curveConfigNames = selector.determineCurveConfigurationsForSecurity(security);

      // todo - we may also want to cache the merged bundle at the level of the curveConfig names
      // e.g. MergedBundleProvider.getMergedBundle(curveConfigNames)
      for (CurveConstructionConfiguration curveConfig : curveConfigNames) {

        Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundle =
            _multicurveBundleProviderFunction.generateBundle(curveConfig);

        if (bundle.isValueAvailable()) {
          Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result = bundle.getValue();
          bundles.add(result.getFirst());
          mergedJacobianBundle.addAll(result.getSecond());
        } else {
          incompleteBundles.add(curveConfig.getName());
        }
      }

      if (!curveConfigNames.isEmpty() && incompleteBundles.isEmpty() && fxmResult.isValueAvailable()) {

        MulticurveProviderDiscount bundle = mergeBundlesAndMatrix(bundles, fxmResult.getValue());
        return success(_factory.createCalculator(security, bundle, mergedJacobianBundle));

      } else if (curveConfigNames.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "No matching curves found for security: {}", security);
      } else if (!incompleteBundles.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "Missing complete curve bundles(s) for: {}", incompleteBundles);
      } else {
        return propagateFailure(fxmResult);
      }
    } else {
      return propagateFailure(mesResult);
    }
  }

  private MulticurveProviderDiscount mergeBundlesAndMatrix(Collection<MulticurveProviderDiscount> providers,
                                                           FXMatrix fxMatrix) {
    // Don't merge when we only have a single provider bundle
    return providers.size() > 1 ?
        ProviderUtils.mergeDiscountingProviders(mergeBundles(providers), fxMatrix) :
        providers.iterator().next();
  }

  private MulticurveProviderDiscount mergeBundles(Collection<MulticurveProviderDiscount> providers) {
    return ProviderUtils.mergeDiscountingProviders(providers);
  }
}
