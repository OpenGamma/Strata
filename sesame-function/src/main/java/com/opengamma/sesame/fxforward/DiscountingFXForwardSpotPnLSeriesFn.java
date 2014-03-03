/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import javax.inject.Inject;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.google.common.base.Optional;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.FXReturnSeriesFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
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

  private final FXReturnSeriesFn _fxReturnSeriesProvider;
  private final HistoricalTimeSeriesFn _historicalTimeSeriesProvider;

  /**
   * How big a timeseries result is required. Start date will be valuation date - period.
   */
  private final Period _seriesPeriod;

  @Inject
  public DiscountingFXForwardSpotPnLSeriesFn(final FXForwardCalculatorFn calculatorProvider,
                                             final CurrencyPairsFn currencyPairsFn,
                                             final Optional<Currency> outputCurrency,
                                             final FXReturnSeriesFn fxReturnSeriesProvider,
                                             final HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             final Period seriesPeriod) {
    _calculatorProvider = calculatorProvider;
    _currencyPairsFn = currencyPairsFn;
    _outputCurrency = outputCurrency;
    _fxReturnSeriesProvider = fxReturnSeriesProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _seriesPeriod = seriesPeriod;
  }

  public DiscountingFXForwardSpotPnLSeriesFn(final FXForwardCalculatorFn calculatorProvider,
                                             final CurrencyPairsFn currencyPairsFn,
                                             final FXReturnSeriesFn fxReturnSeriesProvider,
                                             final HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             final Period seriesPeriod) {
    this(calculatorProvider,
         currencyPairsFn,
         Optional.<Currency>absent(),
         fxReturnSeriesProvider,
         historicalTimeSeriesProvider,
         seriesPeriod);
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> calculatePnlSeries(final FXForwardSecurity security, LocalDate endDate) {

    final Currency payCurrency = security.getPayCurrency();
    final Currency receiveCurrency = security.getReceiveCurrency();

    final UnorderedCurrencyPair pair = UnorderedCurrencyPair.of(payCurrency, receiveCurrency);
    final Result<CurrencyPair> cpResult = _currencyPairsFn.getCurrencyPair(pair);

    final Result<FXForwardCalculator> calculatorResult = _calculatorProvider.generateCalculator(security);

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

    if (calculatorResult.isValueAvailable()) {

      final MultipleCurrencyAmount currencyExposure = calculatorResult.getValue().calculateCurrencyExposure();

      if (cpResult.isValueAvailable()) {

        final CurrencyPair currencyPair = cpResult.getValue();

        LocalDateRange dateRange = LocalDateRange.of(endDate.minus(_seriesPeriod), endDate, true);

        final Result<LocalDateDoubleTimeSeries> returnSeriesResult =
            _fxReturnSeriesProvider.calculateReturnSeries(dateRange, currencyPair);
        final LocalDateDoubleTimeSeries fxSpotReturnSeries = returnSeriesResult.getValue();

        final Currency baseCurrency = currencyPair.getBase();
        final double exposure = currencyExposure.getAmount(currencyPair.getCounter());

        if (conversionIsRequired(baseCurrency)) {

          final Result<LocalDateDoubleTimeSeries> conversionSeriesResult =
              _historicalTimeSeriesProvider.getHtsForCurrencyPair(CurrencyPair.of(baseCurrency, _outputCurrency.get()),
                                                                  endDate);

          if (conversionSeriesResult.isValueAvailable()) {

            final LocalDateDoubleTimeSeries conversionSeries = conversionSeriesResult.getValue();
            final LocalDateDoubleTimeSeries convertedSeries = conversionSeries.multiply(exposure);
            return success(convertedSeries.multiply(fxSpotReturnSeries));

          } else {
            return propagateFailure(conversionSeriesResult);
          }
        } else {
          return success(fxSpotReturnSeries.multiply(exposure));
        }
      } else {
        return propagateFailure(cpResult);
      }
    } else {
      return propagateFailure(calculatorResult);
    }
  }

  private boolean conversionIsRequired(final Currency baseCurrency) {

    // No output currency property or it's the same as base
    return _outputCurrency.or(baseCurrency) != baseCurrency;
  }
}
