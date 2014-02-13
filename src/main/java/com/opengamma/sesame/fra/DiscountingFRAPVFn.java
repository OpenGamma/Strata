/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.financial.analytics.conversion.FRASecurityConverter;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.MarketExposureSelector;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;


/**
 * Calculate discounting PV for a FRA.
 */
public class DiscountingFRAPVFn implements FRAPVFn {

  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();
  private final FRASecurityConverter _converter;
  private final ValuationTimeFn _valuationTimeFn;
  private final MarketExposureSelectorFn _marketExposureSelectorFn;
  private final DiscountingMulticurveBundleFn _multicurveBundleProviderFunction;

  public DiscountingFRAPVFn(FRASecurityConverter converter,
                            ValuationTimeFn valuationTimeFn,
                            MarketExposureSelectorFn marketExposureSelectorFn,
                            DiscountingMulticurveBundleFn multicurveBundleProviderFunction) {
    _converter = converter;
    _valuationTimeFn = valuationTimeFn;
    _marketExposureSelectorFn = marketExposureSelectorFn;
    _multicurveBundleProviderFunction = multicurveBundleProviderFunction;
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(FRASecurity security) {
    InstrumentDefinition<?> definition = security.accept(_converter);
    InstrumentDerivative instrumentDerivative = definition.toDerivative(_valuationTimeFn.getTime());
    Result<MarketExposureSelector> mesResult = _marketExposureSelectorFn.getMarketExposureSelector();

    if (mesResult.isValueAvailable()) {
      Set<String> incompleteBundles = new HashSet<>();
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
          incompleteBundles.add(curveConfig.getName());
        }
      }

      if (!curveConfigs.isEmpty() && incompleteBundles.isEmpty()) {
        MulticurveProviderDiscount bundle = mergeBundlesAndMatrix(bundles, new FXMatrix());
        return success(instrumentDerivative.accept(PVDC, bundle));
      } else if (curveConfigs.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "No matching curves found for security: {}", security);
      } else {
        return failure(FailureStatus.MISSING_DATA, "Missing complete curve bundles(s) for: {}", incompleteBundles);
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
