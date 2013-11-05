/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.StandardResultGenerator.failure;
import static com.opengamma.sesame.StandardResultGenerator.propagateFailure;
import static com.opengamma.sesame.StandardResultGenerator.success;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.analytics.model.forex.FXUtils;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;

public class DiscountingFXForwardPV implements FXForwardPVFunction {

  /** The present value calculator */
  private static final InstrumentDerivativeVisitor<MulticurveProviderInterface, MultipleCurrencyAmount> CALCULATOR =
      PresentValueDiscountingCalculator.getInstance();

  private final FXMatrixProvider _fxMatrixProvider;

  private final FinancialSecurityVisitor<InstrumentDefinition<?>> _securityConverter;

  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  private final InstrumentExposuresProvider _instrumentExposuresProvider;
  private final DiscountingMulticurveBundleProviderFunction _multicurveBundleProviderFunction;
  private final ValuationTimeProviderFunction _valuationTimeProviderFunction;

  public DiscountingFXForwardPV(FXMatrixProvider fxMatrixProvider,
                                FinancialSecurityVisitor<InstrumentDefinition<?>> securityConverter,
                                FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                InstrumentExposuresProvider instrumentExposuresProvider,
                                DiscountingMulticurveBundleProviderFunction multicurveBundleProviderFunction,
                                ValuationTimeProviderFunction valuationTimeProviderFunction) {

    _fxMatrixProvider = fxMatrixProvider;
    _securityConverter = securityConverter;
    _definitionToDerivativeConverter = definitionToDerivativeConverter;
    _instrumentExposuresProvider = instrumentExposuresProvider;
    _multicurveBundleProviderFunction = multicurveBundleProviderFunction;
    _valuationTimeProviderFunction = valuationTimeProviderFunction;
  }

  @Override
  public FunctionResult<CurrencyLabelledMatrix1D> calculatePV(FXForwardSecurity security) {

    ZonedDateTime valuationTime = _valuationTimeProviderFunction.getZonedDateTime();

    // get currencies from security, could use visitor/utils but ...
    Set<Currency> currencies = ImmutableSet.of(security.getPayCurrency(), security.getReceiveCurrency());

    // Even if we can't get a matrix we want to get as afar as we can to
    // ensure market data population, so ignore the result for now
    FunctionResult<FXMatrix> fxmResult = _fxMatrixProvider.getFXMatrix(currencies);

    // Determine required exposure function
    Set<String> exposureConfigNames = ImmutableSet.of("some sensible default");

    Set<String> incompleteBundles = new HashSet<>();
    Set<MulticurveProviderDiscount> bundles = new HashSet<>();

    // We allow for there being more than one exposure config, though in
    // reality there will probably be only one
    for (final String curveExposureConfig : exposureConfigNames) {

      final Set<String> curveConstructionConfigurationNames =
          _instrumentExposuresProvider.getCurveConstructionConfigurationsForConfig(curveExposureConfig, security);

      for (final String curveConstructionConfigurationName : curveConstructionConfigurationNames) {

        FunctionResult<MulticurveProviderDiscount> bundle =
            _multicurveBundleProviderFunction.generateBundle(curveConstructionConfigurationName);

        if (bundle.isResultAvailable()) {
          bundles.add(bundle.getResult());
        } else {
          incompleteBundles.add(curveConstructionConfigurationName);
        }
      }
    }

    if (incompleteBundles.isEmpty() && fxmResult.isResultAvailable()) {

      // Generate Instrument Derivative from security
      InstrumentDefinition<?> definition = security.accept(_securityConverter);

      MulticurveProviderDiscount bundle = mergeBundles(fxmResult.getResult(), bundles);

      // todo - what do we really need in terms of time series?
      InstrumentDerivative derivative =
          _definitionToDerivativeConverter.convert(security, definition, valuationTime, new HistoricalTimeSeriesBundle());
      MultipleCurrencyAmount mca = derivative.accept(CALCULATOR, bundle);

      return success(FXUtils.getMultipleCurrencyAmountAsMatrix(mca));
    } else if (!incompleteBundles.isEmpty()) {
      return failure(FailureStatus.MISSING_DATA, "Missing curveConstructionConfiguration(s) for: {}", incompleteBundles);
    } else {
      return propagateFailure(fxmResult);
    }
  }

  private MulticurveProviderDiscount mergeBundles(FXMatrix fxMatrix, Collection<MulticurveProviderDiscount> providers) {
    final MulticurveProviderDiscount result = ProviderUtils.mergeDiscountingProviders(providers);
    return ProviderUtils.mergeDiscountingProviders(result, fxMatrix);
  }
}
