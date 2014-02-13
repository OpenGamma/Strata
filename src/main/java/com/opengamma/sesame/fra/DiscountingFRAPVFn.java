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

import org.apache.tools.ant.types.Mapper;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorAdapter;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParRateDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
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
 * Calculate discounting PV and par rate for a FRA.
 */
public class DiscountingFRAPVFn implements FRAPVFn {

  /**
   * Calculator for present value.
   */
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();

  /**
   * Calculator for par rate.
   */
  private static final ParRateDiscountingCalculator PRDC = ParRateDiscountingCalculator.getInstance();

  /**
   * Converter for a FRA.
   */
  private final FRASecurityConverter _converter;

  /**
   * Provides the valuation time to perform calculations as at.
   */
  private final ValuationTimeFn _valuationTimeFn;

  /**
   * Generates the market exposure selector. In turn this can be used to get
   * an ExposureFunction.
   */
  private final MarketExposureSelectorFn _marketExposureSelectorFn;

  /**
   * Generates a disccounting multicurve bundle.
   */
  private final DiscountingMulticurveBundleFn _multicurveBundleProviderFunction;

  /**
   * Create the function.
   *
   * @param converter converter for a FRA
   * @param valuationTimeFn provides the valuation time to perform calculations as at
   * @param marketExposureSelectorFn generates the market exposure selector
   * @param multicurveBundleProviderFunction generates a disccounting multicurve bundle
   */
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
  public Result<Double> calculateRate(FRASecurity security) {
    return calculateResult(security, PRDC);
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(FRASecurity security) {
    return calculateResult(security, PVDC);
  }

  private <T> Result<T> calculateResult(FRASecurity security,
                                        InstrumentDerivativeVisitorAdapter<MulticurveProviderInterface, T> calculator) {
    Result<MulticurveProviderDiscount> bundleResult = createBundle(security);
    if (bundleResult.isValueAvailable()) {
      return success(calculateResult(security, bundleResult.getValue(), calculator));
    } else {
      return propagateFailure(bundleResult);
    }
  }

  private <T> T calculateResult(FRASecurity security,
                                 MulticurveProviderDiscount bundle,
                                 InstrumentDerivativeVisitorAdapter<MulticurveProviderInterface, T> calculator) {
    InstrumentDerivative instrumentDerivative = createInstrumentDerivative(security);
    return instrumentDerivative.accept(calculator, bundle);
  }

  private InstrumentDerivative createInstrumentDerivative(FRASecurity security) {
    InstrumentDefinition<?> definition = security.accept(_converter);
    return definition.toDerivative(_valuationTimeFn.getTime());
  }

  private Result<MulticurveProviderDiscount> createBundle(FRASecurity security) {

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
        return success(mergeBundlesAndMatrix(bundles, new FXMatrix()));
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
