/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import java.util.LinkedList;

import javax.inject.Inject;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.FXReturnSeriesFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
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
  private final FXMatrixFn _fxMatrixFn;

  private final FXReturnSeriesFn _fxReturnSeriesProvider;
  private final HistoricalTimeSeriesFn _historicalTimeSeriesProvider;

  /**
   * How big a timeseries result is required.
   */
  private final LocalDateRange _dateRange;
  
  @Inject
  public DiscountingFXForwardSpotPnLSeriesFn(final FXForwardCalculatorFn calculatorProvider,
                                             final CurrencyPairsFn currencyPairsFn,
                                             final Optional<Currency> outputCurrency,
                                             final Boolean useHistoricalSpot,
                                             final FXReturnSeriesFn fxReturnSeriesProvider,
                                             final HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             final FXMatrixFn fxMatrixFn, 
                                             final LocalDateRange dateRange) {
    _calculatorProvider = calculatorProvider;
    _currencyPairsFn = currencyPairsFn;
    _outputCurrency = outputCurrency;
    _useHistoricalSpot = useHistoricalSpot;
    _fxReturnSeriesProvider = fxReturnSeriesProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _fxMatrixFn = fxMatrixFn;
    _dateRange = dateRange;
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

    // todo this if/else nesting is fairly horrible - is there a nicer way? E.g:
      //return gatherResults(cpResult, returnSeriesResult, calculatorResult).whenAvailable(new Doer() {
    //
    //  public Result doIt(CurrencyPair pair, LocalDateDoubleTimeSeries series, FxForwardCalculator calculator) {
    //    Result<LocalDateDoubleTimeSeries> conversionSeriesResult = _historicalTimeSeriesProvider.getHtsForCurrencyPair(
    //        CurrencyPair.of(baseCurrency, _outputCurrency.get()));
    //
    //    final Currency baseCurrency = pair.getBase();
    //    final double exposure = mca.getAmount(pair.getCounter());
    //    final LocalDateDoubleTimeSeries conversionSeries = conversionSeriesResult.getValue();
    //
    //    if (conversionSeriesResult.isValueAvailable()) {
    //
    //      final LocalDateDoubleTimeSeries convertedSeries = conversionSeries.multiply(exposure);
    //      return success(convertedSeries.multiply(series));
    //
    //    } else {
    //      return propagateFailure(conversionSeriesResult);
    //    }
    //  }
    //});
    // or "flatMap that shit!"

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

        final Result<LocalDateDoubleTimeSeries> returnSeriesResult =
            _fxReturnSeriesProvider.calculateReturnSeries(env, adjustedRange, currencyPair);
        LocalDateDoubleTimeSeries fxSpotReturnSeries = trimReturnSeries(returnSeriesResult.getValue());
        
        final Currency baseCurrency = currencyPair.getBase();
        final double exposure = currencyExposure.getAmount(currencyPair.getCounter());

        if (!_useHistoricalSpot) {
          LocalDateDoubleTimeSeries ccyPairHts = _historicalTimeSeriesProvider.getHtsForCurrencyPair(env, currencyPair, adjustedRange).getValue().reciprocal();
          FXMatrix fxMatrix = _fxMatrixFn.getFXMatrix(env, Sets.newHashSet(payCurrency, receiveCurrency)).getValue();
          double envFxRate = fxMatrix.getFxRate(currencyPair.getBase(), currencyPair.getCounter());
          
          fxSpotReturnSeries = fxSpotReturnSeries.multiply(ccyPairHts).divide(envFxRate);
        }
        
        if (conversionIsRequired(baseCurrency)) {

          CurrencyPair outputPair = CurrencyPair.of(baseCurrency, _outputCurrency.get());
          final Result<LocalDateDoubleTimeSeries> conversionSeriesResult =
              _historicalTimeSeriesProvider.getHtsForCurrencyPair(env, outputPair, endDate);

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
  
  private LocalDateDoubleTimeSeries trimReturnSeries(LocalDateDoubleTimeSeries ts) {
    LinkedList<LocalDate> dates = Lists.newLinkedList();
    LinkedList<Double> values = Lists.newLinkedList();
    
    for (int j = ts.size() - 1; j >= 0; j--) {
      LocalDate date = ts.getTimeAtIndex(j);
      if (date.isBefore(_dateRange.getStartDateInclusive())) {
        break;
      }
      Double value = ts.getValueAtIndex(j);
      dates.addFirst(date);
      values.addFirst(value);
    }

    return ImmutableLocalDateDoubleTimeSeries.of(dates, values);
  }

}
