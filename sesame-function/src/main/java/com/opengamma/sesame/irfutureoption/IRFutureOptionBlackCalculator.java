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

public class IRFutureOptionBlackCalculator implements IRFutureOptionCalculator {

  private static final PresentValueBlackSTIRFutureOptionCalculator PV_CALC = PresentValueBlackSTIRFutureOptionCalculator.getInstance();
  
  private static final PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator PV01_CALC = PresentValueCurveSensitivityBlackSTIRFutureOptionCalculator.getInstance();
  
  private final InstrumentDerivative _derivative;
  
  private final BlackSTIRFuturesProviderInterface _black;
  
  public IRFutureOptionBlackCalculator(IRFutureOptionTrade trade,
                                       InterestRateFutureOptionTradeConverter converter,
                                       BlackSTIRFuturesProviderInterface black,
                                       ZonedDateTime valTime,
                                       FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                       HistoricalTimeSeriesBundle fixings) {
    _derivative = createInstrumentDerivative(trade, converter, valTime, definitionToDerivativeConverter, fixings);
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
