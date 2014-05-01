/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.future.calculator.FuturesPriceMulticurveCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.analytics.conversion.DeliverableSwapFutureTradeConverter;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.util.result.Result;

/**
 * Calculator for deliverable swap futures using the discounting method.
 */
public class DeliverableSwapFutureDiscountingCalculator implements DeliverableSwapFutureCalculator {
  
  /**
   * Calculator for the security model price.
   */
  private static final FuturesPriceMulticurveCalculator MQDC = FuturesPriceMulticurveCalculator.getInstance();
  
  /**
   * Derivative form of the security.
   */
  private final InstrumentDerivative _derivative;
  
  /**
   * The multicurve bundle.
   */
  private final MulticurveProviderInterface _multicurve;
  
  /**
   * Constructs a calculator using the discounting method.
   * @param trade the trade to calculate results on.
   * @param multicurve the multicurve used to calculate results from.
   * @param converter the converter used to create the definition form of the trade.
   * @param valDateTime the valuation time.
   * @param definitionToDerivativeConverter the converter used to create the derivative form of the trade.
   * @param tsBundle the time series bundle containing the last margin price of the future.
   */
  public DeliverableSwapFutureDiscountingCalculator(DeliverableSwapFutureTrade trade,
                                                    MulticurveProviderInterface multicurve,
                                                    DeliverableSwapFutureTradeConverter converter,
                                                    ZonedDateTime valDateTime,
                                                    FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                    HistoricalTimeSeriesBundle tsBundle) {
    _derivative = createInstrumentDerivative(trade, converter, valDateTime, definitionToDerivativeConverter, tsBundle);
    _multicurve = multicurve;
  }
 
  @Override
  public Result<Double> calculateSecurityModelPrice() {
    return Result.success(_derivative.accept(MQDC, _multicurve));
  }
  
  /**
   * Create the OG-Analytics derivative representation of a deliverable swap future from the given trade.
   * @param deliverableSwapFutureTrade the trade to convert to an OG-Analytics derivative.
   * @param converter the converter used to create the deliverable swap future definition.
   * @param valuationTime the valuation time at which to create the derivative.
   * @param definitionToDerivativeConverter the converter used to convert from the definition to derivative.
   * @param tsBundle the time series bundle containing the last margin price of the future.
   * @return the derivative representation of the deliverable swap future trade.
   */
  private InstrumentDerivative createInstrumentDerivative(DeliverableSwapFutureTrade deliverableSwapFutureTrade,
                                                          DeliverableSwapFutureTradeConverter converter,
                                                          ZonedDateTime valuationTime,
                                                          FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                          HistoricalTimeSeriesBundle tsBundle) {
    InstrumentDefinition<?> definition = converter.convert(deliverableSwapFutureTrade);
    return definitionToDerivativeConverter.convert(deliverableSwapFutureTrade.getSecurity(), definition, valuationTime, tsBundle);
  }
  
  
  
}
