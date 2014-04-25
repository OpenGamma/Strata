/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitorAdapter;
import com.opengamma.analytics.financial.provider.calculator.discounting.MarketQuoteDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.analytics.conversion.DeliverableSwapFutureTradeConverter;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.util.result.Result;

/**
 * Default calculator for deliverable swap futures.
 */
public class DeliverableSwapFutureDiscountingCalculator implements DeliverableSwapFutureCalculator {

  /**
   * Calculator for PV.
   */
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();
  
  
  private static final MarketQuoteDiscountingCalculator MQDC = MarketQuoteDiscountingCalculator.getInstance();
  
  /**
   * Derivative form of the security.
   */
  private final InstrumentDerivative _derivative;
  
  /**
   * The multicurve bundle.
   */
  private final MulticurveProviderInterface _bundle;
  
  
  public DeliverableSwapFutureDiscountingCalculator(DeliverableSwapFutureTrade trade,
                                                    MulticurveProviderInterface bundle,
                                                    DeliverableSwapFutureTradeConverter converter,
                                                    ZonedDateTime valDateTime,
                                                    FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                    HistoricalTimeSeriesBundle fixings) {
    _derivative = createInstrumentDerivative(trade, converter, valDateTime, definitionToDerivativeConverter, fixings);
    _bundle = bundle;
  } 
  private <T> T calculateResult(InstrumentDerivativeVisitorAdapter<MulticurveProviderInterface, T> calculator) {
    return _derivative.accept(calculator, _bundle);
  }
 
  @Override
  public Result<Double> calculateSecurityModelPrice() {
    return Result.success(calculateResult(MQDC));
  }
  
  private InstrumentDerivative createInstrumentDerivative(DeliverableSwapFutureTrade deliverableSwapFutureTrade,
      DeliverableSwapFutureTradeConverter converter,
      ZonedDateTime valuationTime,
      FixedIncomeConverterDataProvider definitionToDerivativeConverter,
      HistoricalTimeSeriesBundle fixings) {
    InstrumentDefinition<?> definition = converter.convert(deliverableSwapFutureTrade);
    return definitionToDerivativeConverter.convert(deliverableSwapFutureTrade.getSecurity(), definition, valuationTime, fixings);
  }
  
  
  
}
