/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import java.util.LinkedList;

import javax.inject.Inject;

import org.threeten.bp.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.TimeSeriesReturnConverter;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.pnl.HistoricalPnLFXConverterFn;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

public class DiscountingFXForwardSpotPnLSeriesFn implements FXForwardPnLSeriesFn {

  private final FXForwardCalculatorFn _calculatorProvider;

  private final CurrencyPairsFn _currencyPairsFn;
  
  /**
   * The requested currency for this P&L series. If not supplied, then the
   * output will be in the base currency of the currency pair corresponding
   * to the FX Forward's currencies.
   */
  private final Optional<Currency> _outputCurrency;
  private final Boolean _useHistoricalSpot;
  private final HistoricalPnLFXConverterFn _pnlFXConverterFn;
  
  /**
   * The time-series converter.
   */
  private final TimeSeriesReturnConverter _timeSeriesConverter;

  
  /**
   * The market data function.
   */
  private final HistoricalMarketDataFn _historicalMarketDataFn;

  /**
   * How big a timeseries result is required.
   */
  private final LocalDateRange _dateRange;
  
  @Inject
  public DiscountingFXForwardSpotPnLSeriesFn(FXForwardCalculatorFn calculatorProvider,
                                             CurrencyPairsFn currencyPairsFn,
                                             Optional<Currency> outputCurrency,
                                             Boolean useHistoricalSpot,
                                             LocalDateRange dateRange,
                                             HistoricalMarketDataFn historicalMarketDataFn,
                                             TimeSeriesReturnConverter timeSeriesConverter,
                                             HistoricalPnLFXConverterFn pnlFXConverterFn) {
    _calculatorProvider = calculatorProvider;
    _currencyPairsFn = currencyPairsFn;
    _outputCurrency = outputCurrency;
    _useHistoricalSpot = useHistoricalSpot;
    _dateRange = dateRange;
    _historicalMarketDataFn = historicalMarketDataFn;
    _timeSeriesConverter = timeSeriesConverter;
    _pnlFXConverterFn = pnlFXConverterFn;
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> calculatePnlSeries(Environment env,
                                                              FXForwardSecurity security,
                                                              LocalDate endDate) {

    final Currency payCurrency = security.getPayCurrency();
    final Currency receiveCurrency = security.getReceiveCurrency();

    final UnorderedCurrencyPair pair = UnorderedCurrencyPair.of(payCurrency, receiveCurrency);
    final Result<CurrencyPair> cpResult = _currencyPairsFn.getCurrencyPair(pair);

    final Result<FXForwardCalculator> calculatorResult = _calculatorProvider.generateCalculator(env, security);

    if (calculatorResult.isSuccess()) {

      final MultipleCurrencyAmount currencyExposure = calculatorResult.getValue().calculateCurrencyExposure(env);

      if (cpResult.isSuccess()) {

        final CurrencyPair currencyPair = cpResult.getValue();
        //take one week off the start date. this ensures that the start of the underlying
        //price series will provide at least one business day of data before the required start of 
        // the return series. the resulting return series is trimmed so that first day of PnL = 
        //start date.
        LocalDate adjustedStart = _dateRange.getStartDateInclusive().minusWeeks(1);
        LocalDateRange adjustedRange = LocalDateRange.of(adjustedStart, _dateRange.getEndDateInclusive(), true);

        Result<LocalDateDoubleTimeSeries> fxSeries = _historicalMarketDataFn.getFxRates(env, currencyPair, adjustedRange);
        
        if (!fxSeries.isSuccess()) {
          return Result.failure(fxSeries);
        }
        
        LocalDateDoubleTimeSeries returnSeries = trimSeries(fxSeries.getValue());
        LocalDateDoubleTimeSeries fxSpotReturnSeries = _timeSeriesConverter.convert(returnSeries);
        
        final Currency baseCurrency = currencyPair.getBase();
        final double exposure = currencyExposure.getAmount(currencyPair.getCounter());

        if (!_useHistoricalSpot) {
          
          Result<LocalDateDoubleTimeSeries> spotConvertedSeriesResult = _pnlFXConverterFn.convertToSpotRate(env, currencyPair, fxSpotReturnSeries);
          if (!spotConvertedSeriesResult.isSuccess()) {
            return Result.failure(spotConvertedSeriesResult);
          }
          
          fxSpotReturnSeries = spotConvertedSeriesResult.getValue();
        }
        
        if (conversionIsRequired(baseCurrency)) {

          CurrencyPair outputPair = CurrencyPair.of(baseCurrency, _outputCurrency.get());
          final Result<LocalDateDoubleTimeSeries> conversionSeriesResult =
              _historicalMarketDataFn.getFxRates(env, outputPair, adjustedRange);

          if (conversionSeriesResult.isSuccess()) {

            final LocalDateDoubleTimeSeries conversionSeries = conversionSeriesResult.getValue();
            final LocalDateDoubleTimeSeries convertedSeries = conversionSeries.multiply(exposure);
            return Result.success(convertedSeries.multiply(fxSpotReturnSeries));

          } else {
            return Result.failure(conversionSeriesResult);
          }
        } else {
          return Result.success(fxSpotReturnSeries.multiply(exposure));
        }
      } else {
        return Result.failure(cpResult);
      }
    } else {
      return Result.failure(calculatorResult);
    }
  }

  private boolean conversionIsRequired(final Currency baseCurrency) {

    // No output currency property or it's the same as base
    return _outputCurrency.or(baseCurrency) != baseCurrency;
  }
  
  private LocalDateDoubleTimeSeries trimSeries(LocalDateDoubleTimeSeries ts) {
    LinkedList<LocalDate> dates = Lists.newLinkedList();
    LinkedList<Double> values = Lists.newLinkedList();
    
    for (int j = ts.size() - 1; j >= 0; j--) {
      LocalDate date = ts.getTimeAtIndex(j);
      Double value = ts.getValueAtIndex(j);
      dates.addFirst(date);
      values.addFirst(value);
      if (date.isBefore(_dateRange.getStartDateInclusive())) {
        break;
      }
    }

    return ImmutableLocalDateDoubleTimeSeries.of(dates, values);
  }

}
