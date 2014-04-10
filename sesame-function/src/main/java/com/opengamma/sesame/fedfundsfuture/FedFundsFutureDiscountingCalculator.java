/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fedfundsfuture;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.financial.analytics.conversion.FederalFundsFutureTradeConverter;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.FutureTradeConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.sesame.trade.FedFundsFutureTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Federal funds futures discounting calculator.
 */
public class FedFundsFutureDiscountingCalculator implements FedFundsFutureCalculator {
  
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();
  
  private final InstrumentDerivative _derivative;
  
  private final MulticurveProviderInterface _bundle;
  
  public FedFundsFutureDiscountingCalculator(FedFundsFutureTrade trade,
                                             MulticurveProviderInterface bundle,
                                             FederalFundsFutureTradeConverter converter,
                                             ZonedDateTime valuationTime,
                                             FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                             HistoricalTimeSeriesBundle fixings) {
    _derivative = createInstrumentDerivative(trade, converter, valuationTime, definitionToDerivativeConverter, fixings);
    _bundle = ArgumentChecker.notNull(bundle, "bundle");
  }
  
  private InstrumentDerivative createInstrumentDerivative(FedFundsFutureTrade trade,
                                                          FederalFundsFutureTradeConverter converter,
                                                          ZonedDateTime valuationTime,
                                                          FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                                          HistoricalTimeSeriesBundle fixings) {
    InstrumentDefinition<?> definition = converter.convert(trade);
    return definitionToDerivativeConverter.convert(trade.getSecurity(), definition, valuationTime, fixings);
  }
  
  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(_derivative.accept(PVDC, _bundle));
  }

}
