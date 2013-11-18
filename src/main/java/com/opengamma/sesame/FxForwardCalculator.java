/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.model.forex.FXUtils;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * A calculation class for a specific FxForward security. On construction it is provided
 * with all the analytics objects it needs to produce its results.
 */
public class FxForwardCalculator {

  /** The analytics present value calculator */
  private static final InstrumentDerivativeVisitor<MulticurveProviderInterface, MultipleCurrencyAmount> CALCULATOR =
      PresentValueDiscountingCalculator.getInstance();

  private final FXForwardSecurity _security;
  private final MulticurveProviderDiscount _discountingMulticurveBundle;

  private final FinancialSecurityVisitor<InstrumentDefinition<?>> _securityConverter;
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;
  private final ValuationTimeProviderFunction _valuationTimeProviderFunction;

  public FxForwardCalculator(FXForwardSecurity security,
                             MulticurveProviderDiscount discountingMulticurveBundle,
                             FinancialSecurityVisitor<InstrumentDefinition<?>> securityConverter,
                             FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                             ValuationTimeProviderFunction valuationTimeProviderFunction) {
    _security = security;
    _securityConverter = securityConverter;
    _discountingMulticurveBundle = discountingMulticurveBundle;
    _definitionToDerivativeConverter = definitionToDerivativeConverter;
    _valuationTimeProviderFunction = valuationTimeProviderFunction;
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

    ZonedDateTime valuationTime = _valuationTimeProviderFunction.getZonedDateTime();
    InstrumentDefinition<?> definition = _security.accept(_securityConverter);
    // Note that no time series are needed for FX Forward, so pass in an empty bundle
    InstrumentDerivative derivative =
        _definitionToDerivativeConverter.convert(_security, definition, valuationTime, new HistoricalTimeSeriesBundle());
    return derivative.accept(CALCULATOR, _discountingMulticurveBundle);
  }
}
