/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.sesame.StandardResultGenerator.failure;
import static com.opengamma.sesame.StandardResultGenerator.propagateFailure;
import static com.opengamma.sesame.StandardResultGenerator.success;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.DiscountingMulticurveBundleProviderFunction;
import com.opengamma.sesame.FXMatrixProviderFunction;
import com.opengamma.sesame.FailureStatus;
import com.opengamma.sesame.FunctionResult;
import com.opengamma.sesame.MarketExposureSelector;
import com.opengamma.sesame.MarketExposureSelectorProvider;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.Pair;

public class FxForwardDiscountingCalculatorProvider implements FxForwardCalculatorProvider {

  private final FxForwardCalculatorFactory _factory;

  private final MarketExposureSelectorProvider _marketExposureSelectorProvider;
  private final FXMatrixProviderFunction _fxMatrixProvider;
  private final DiscountingMulticurveBundleProviderFunction _multicurveBundleProviderFunction;

  public FxForwardDiscountingCalculatorProvider(FxForwardCalculatorFactory factory,
                                                MarketExposureSelectorProvider marketExposureSelectorProvider,
                                                FXMatrixProviderFunction fxMatrixProvider,
                                                DiscountingMulticurveBundleProviderFunction multicurveBundleProviderFunction) {
    _factory = factory;
    _marketExposureSelectorProvider = marketExposureSelectorProvider;
    _fxMatrixProvider = fxMatrixProvider;
    _multicurveBundleProviderFunction = multicurveBundleProviderFunction;
  }

  @Override
  public FunctionResult<FxForwardCalculator> generateCalculator(FXForwardSecurity security) {

    // get currencies from security, probably should use visitor/utils
    Set<Currency> currencies = ImmutableSet.of(security.getPayCurrency(), security.getReceiveCurrency());

    // Even if we can't get a matrix we want to get as afar as we can to
    // ensure market data population, so ignore the result for now
    FunctionResult<FXMatrix> fxmResult = _fxMatrixProvider.getFXMatrix(currencies);

    FunctionResult<MarketExposureSelector> mesResult = _marketExposureSelectorProvider.getMarketExposureSelector();

    if (mesResult.isResultAvailable()) {

      Set<String> incompleteBundles = new HashSet<>();
      Set<MulticurveProviderDiscount> bundles = new HashSet<>();
      CurveBuildingBlockBundle mergedJacobianBundle = new CurveBuildingBlockBundle();

      MarketExposureSelector selector = mesResult.getResult();
      Set<String> curveConfigNames = selector.determineCurveConfigurationsForSecurity(security);

      for (String name : curveConfigNames) {

        FunctionResult<Pair<MulticurveProviderDiscount,CurveBuildingBlockBundle>> bundle =
            _multicurveBundleProviderFunction.generateBundle(name);

        if (bundle.isResultAvailable()) {
          Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result = bundle.getResult();
          bundles.add(result.getFirst());
          mergedJacobianBundle.addAll(result.getSecond());
        } else {
          incompleteBundles.add(name);
        }
      }

      if (incompleteBundles.isEmpty() && fxmResult.isResultAvailable()) {

        MulticurveProviderDiscount bundle = mergeBundles(fxmResult.getResult(), bundles);
        return success(_factory.createCalculator(security, bundle, mergedJacobianBundle));

      } else if (!incompleteBundles.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "Missing complete curve bundles(s) for: {}", incompleteBundles);
      } else {
        return propagateFailure(fxmResult);
      }
    } else {
      return propagateFailure(mesResult);
    }
  }

  private MulticurveProviderDiscount mergeBundles(FXMatrix fxMatrix, Collection<MulticurveProviderDiscount> providers) {
    final MulticurveProviderDiscount result = ProviderUtils.mergeDiscountingProviders(providers);
    return ProviderUtils.mergeDiscountingProviders(result, fxMatrix);
  }
}
