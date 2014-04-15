/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfuture;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.instrument.future.BondFuturesTransactionDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorDelegate;
import com.opengamma.analytics.financial.provider.calculator.discounting.PV01CurveParametersCalculator;
import com.opengamma.analytics.financial.provider.calculator.issuer.PresentValueCurveSensitivityIssuerCalculator;
import com.opengamma.analytics.financial.provider.calculator.issuer.PresentValueIssuerCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.conversion.BondAndBondFutureTradeWithEntityConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.sesame.trade.BondFutureTrade;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Discounting calculator for bond futures.
 */
public class BondFutureDiscountingCalculator implements BondFutureCalculator {

  private static final PresentValueIssuerCalculator PVIC = PresentValueIssuerCalculator.getInstance();
  
  private static final InstrumentDerivativeVisitor<ParameterIssuerProviderInterface, ReferenceAmount<Pair<String, Currency>>> PV01C =
      new PV01CurveParametersCalculator<>(PresentValueCurveSensitivityIssuerCalculator.getInstance());
  
  private final InstrumentDerivative _derivative;
  
  private final ParameterIssuerProviderInterface _curves;
  
  public BondFutureDiscountingCalculator(BondFutureTrade bondFutureTrade,
                                         ParameterIssuerProviderInterface curves,
                                         BondAndBondFutureTradeWithEntityConverter bondFutureTradeConverter,
                                         ZonedDateTime valuationTime,
                                         HistoricalTimeSeriesBundle fixings) {
    _derivative = createInstrumentDerivative(bondFutureTrade, bondFutureTradeConverter, valuationTime, fixings);
    _curves = curves;
  }
  
  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(calculateResult(PVIC));
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01() {
    return Result.success(calculateResult(PV01C));
  }

  private <T> T calculateResult(InstrumentDerivativeVisitorDelegate<ParameterIssuerProviderInterface, T> calculator) {
    return _derivative.accept(calculator, _curves);
  }

  private ReferenceAmount<Pair<String, Currency>> calculateResult(InstrumentDerivativeVisitor<ParameterIssuerProviderInterface, ReferenceAmount<Pair<String, Currency>>> calculator) {
    return _derivative.accept(calculator, _curves);
  }
  
  private InstrumentDerivative createInstrumentDerivative(BondFutureTrade bondFutureTrade,
                                                          BondAndBondFutureTradeWithEntityConverter converter,
                                                          ZonedDateTime valuationTime,
                                                          HistoricalTimeSeriesBundle fixings) {
    final FinancialSecurity security = (FinancialSecurity) bondFutureTrade.getSecurity();
    HistoricalTimeSeries fixingsTS = fixings.get(MarketDataRequirementNames.MARKET_VALUE, security.getExternalIdBundle());
    double lastMarginPrice = fixingsTS.getTimeSeries().getLatestValue();
    InstrumentDefinition<?> definition = converter.convert(bondFutureTrade);
    return ((BondFuturesTransactionDefinition) definition).toDerivative(valuationTime, lastMarginPrice);
  }
}
