/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.MarketQuoteSensitivityBlockCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.parameter.ParameterSensitivityParameterCalculator;
import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.model.forex.FXUtils;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * A calculation class for a specific FxForward security. On construction it is provided
 * with all the analytics objects it needs to produce its results.
 */
public class FXForwardCalculator {

  private static final HistoricalTimeSeriesBundle EMPTY_TIME_SERIES_BUNDLE = new HistoricalTimeSeriesBundle();

  /** The curve sensitivity calculator */
  private static final InstrumentDerivativeVisitor<MulticurveProviderInterface, MultipleCurrencyMulticurveSensitivity> PVCSDC =
      PresentValueCurveSensitivityDiscountingCalculator.getInstance();
  /** The parameter sensitivity calculator */
  private static final ParameterSensitivityParameterCalculator<MulticurveProviderInterface> PSC =
      new ParameterSensitivityParameterCalculator<>(PVCSDC);
  /** The market quote sensitivity calculator */
  private static final MarketQuoteSensitivityBlockCalculator<MulticurveProviderInterface> SENSITIVITY_CALCULATOR =
      new MarketQuoteSensitivityBlockCalculator<>(PSC);

  /** The analytics present value calculator */
  private static final InstrumentDerivativeVisitor<MulticurveProviderInterface, MultipleCurrencyAmount> PV_CALCULATOR =
      PresentValueDiscountingCalculator.getInstance();

  private final FXForwardSecurity _security;
  private final MulticurveProviderDiscount _discountingMulticurveBundle;

  private final CurveBuildingBlockBundle _jacobianBundle;
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  private final ValuationTimeFn _valuationTimeFn;
  private final InstrumentDefinition<?> _instrumentDefinition;

  public FXForwardCalculator(FXForwardSecurity security,
                             MulticurveProviderDiscount discountingMulticurveBundle,
                             CurveBuildingBlockBundle jacobianBundle,
                             FinancialSecurityVisitor<InstrumentDefinition<?>> securityConverter,
                             FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                             ValuationTimeFn valuationTimeFn) {
    _security = security;
    _jacobianBundle = jacobianBundle;
    _discountingMulticurveBundle = discountingMulticurveBundle;
    _definitionToDerivativeConverter = definitionToDerivativeConverter;
    _valuationTimeFn = valuationTimeFn;
    _instrumentDefinition = _security.accept(securityConverter);
  }

  /**
   * Calculate the present value of the FX Forward.
   *
   * @return the present value, not null
   */
  public CurrencyLabelledMatrix1D calculatePV() {
    return FXUtils.getMultipleCurrencyAmountAsMatrix(calculateCurrencyExposure());
  }

  public MultipleCurrencyAmount calculateCurrencyExposure() {

    InstrumentDerivative derivative = generateInstrumentDerivative();
    return derivative.accept(PV_CALCULATOR, _discountingMulticurveBundle);
  }

  // Note that this is one possible implementation (corresponds to DiscountingBCSFunction), we will
  // need to support others (e.g. FXForwardPointsBCSFunction)
  public MultipleCurrencyParameterSensitivity generateBlockCurveSensitivities() {
    return SENSITIVITY_CALCULATOR.fromInstrument(generateInstrumentDerivative(),
                                                 _discountingMulticurveBundle,
                                                 _jacobianBundle);
  }

  // todo - if this class is thrown away after each cycle then we can use a field rather than this method
  private InstrumentDerivative generateInstrumentDerivative() {

    ZonedDateTime valuationTime = _valuationTimeFn.getTime();
    // Note that no time series are needed for FX Forward, so pass in an empty bundle
    return _definitionToDerivativeConverter.convert(
        _security, _instrumentDefinition, valuationTime, EMPTY_TIME_SERIES_BUNDLE);
  }
}
