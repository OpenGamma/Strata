/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PresentValueBlackSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.blackstirfutures.PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.InterestRateFutureOptionTradeConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.sesame.trade.IRFutureOptionTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Interest rate future option Black calculator.
 */
public class IRFutureOptionBlackCalculator implements IRFutureOptionCalculator {

  /**
   * Calculator for the present value of the interest rate future option.
   */
  private static final PresentValueBlackSTIRFutureOptionCalculator PV_CALC = PresentValueBlackSTIRFutureOptionCalculator.getInstance();
  
  /**
   * Calculator for the PV01 of the interest rate future option.
   */
  private static final PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator PV01_CALC = PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator.getInstance();
  
  /**
   * Derivative form on the security.
   */
  private final InstrumentDerivative _derivative;
  
  /**
   * Black volatility data.
   */
  private final BlackSTIRFuturesProviderInterface _black;
  
  /**
   * Constructs a calculator for interest rate future options using the Black model.
   * @param trade the interest rate future option trade, not null.
   * @param converter the converter used to create the definition of the interest rate future option, not null.
   * @param black the Black volatility data, not null.
   * @param valTime the valuation date time, not null.
   * @param definitionToDerivativeConverter the converter used to create the derivative form of the interest rate future option, not null.
   * @param fixings the historical prices of the underlying interest rate future, not null.
   */
  public IRFutureOptionBlackCalculator(IRFutureOptionTrade trade,
                                       InterestRateFutureOptionTradeConverter converter,
                                       BlackSTIRFuturesProviderInterface black,
                                       ZonedDateTime valTime,
                                       FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                       HistoricalTimeSeriesBundle fixings) {
    _derivative = createInstrumentDerivative(ArgumentChecker.notNull(trade, "trade"),
                                             ArgumentChecker.notNull(converter, "converter"),
                                             ArgumentChecker.notNull(valTime, "valTime"),
                                             ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter"),
                                             ArgumentChecker.notNull(fixings, "fixings"));
    _black = ArgumentChecker.notNull(black, "black");
  }
  
  private InstrumentDerivative createInstrumentDerivative(IRFutureOptionTrade trade,
                                                          InterestRateFutureOptionTradeConverter converter,
                                                          ZonedDateTime valTime,
                                                          FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                          HistoricalTimeSeriesBundle fixings) {
    InstrumentDefinition<?> definition = converter.convert(trade);
    return definitionToDerivativeConverter.convert(trade.getSecurity(), definition, valTime, fixings);
  }
  
  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(_derivative.accept(PV_CALC, _black));
  }

  @Override
  public Result<MultipleCurrencyMulticurveSensitivity> calculatePV01() {
    return Result.success(_derivative.accept(PV01_CALC, _black));
  }
}
