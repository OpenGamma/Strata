/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.sesame.StandardResultGenerator.propagateFailure;
import static com.opengamma.sesame.StandardResultGenerator.success;

import javax.inject.Inject;

import org.threeten.bp.Period;

import com.google.common.base.Optional;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.CurrencyPairsFunction;
import com.opengamma.sesame.FunctionResult;
import com.opengamma.sesame.FxReturnSeriesProviderFunction;
import com.opengamma.sesame.HistoricalTimeSeriesProviderFunction;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.money.UnorderedCurrencyPair;

public class DiscountingFxForwardPnLSeries implements FxForwardPnLSeriesFunction {

  private final FxForwardCalculatorProvider _calculatorProvider;

  private final CurrencyPairsFunction _currencyPairsFunction;

  /**
   * The requested currency for this P&L series. If not supplied, then the
   * output will be in the base currency of the currency pair corresponding
   * to the FX Forward's currencies.
   */
  private final Optional<Currency> _outputCurrency;

  private final FxReturnSeriesProviderFunction _fxReturnSeriesProvider;
  private final HistoricalTimeSeriesProviderFunction _historicalTimeSeriesProvider;

  /**
   * How big a timeseries result is required. Start date will be valuation date - period.
   */
  private final Period _seriesPeriod;

  // todo - what should we be injecting?
  @Inject
  public DiscountingFxForwardPnLSeries(FxForwardCalculatorProvider calculatorProvider,
                                       CurrencyPairsFunction currencyPairsFunction,
                                       Optional<Currency> outputCurrency,
                                       FxReturnSeriesProviderFunction fxReturnSeriesProvider,
                                       HistoricalTimeSeriesProviderFunction historicalTimeSeriesProvider, Period seriesPeriod) {
    _calculatorProvider = calculatorProvider;
    _currencyPairsFunction = currencyPairsFunction;
    _outputCurrency = outputCurrency;
    _fxReturnSeriesProvider = fxReturnSeriesProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _seriesPeriod = seriesPeriod;
  }

  public DiscountingFxForwardPnLSeries(FxForwardCalculatorProvider calculatorProvider,
                                       CurrencyPairsFunction currencyPairsFunction,
                                       FxReturnSeriesProviderFunction fxReturnSeriesProvider,
                                       HistoricalTimeSeriesProviderFunction historicalTimeSeriesProvider, Period seriesPeriod) {
    this(calculatorProvider, currencyPairsFunction, Optional.<Currency>absent(), fxReturnSeriesProvider,
         historicalTimeSeriesProvider, seriesPeriod);
  }

  @Override
  public FunctionResult<LocalDateDoubleTimeSeries> calculatePnlSeries(FXForwardSecurity security) {

    final Currency payCurrency = security.getPayCurrency();
    final Currency receiveCurrency = security.getReceiveCurrency();

    UnorderedCurrencyPair pair = UnorderedCurrencyPair.of(payCurrency, receiveCurrency);
    final FunctionResult<CurrencyPair> cpResult = _currencyPairsFunction.getCurrencyPair(pair);

    FunctionResult<FxForwardCalculator> calculatorResult = _calculatorProvider.generateCalculator(security);

    // todo this if/else nesting is fairly horrible - is there a nicer way? E.g:
    //return gatherResults(cpResult, returnSeriesResult, calculatorResult).whenAvailable(new Doer() {
    //
    //  public FunctionResult doIt(CurrencyPair pair, LocalDateDoubleTimeSeries series, FxForwardCalculator calculator) {
    //    FunctionResult<LocalDateDoubleTimeSeries> conversionSeriesResult = _historicalTimeSeriesProvider.getHtsForCurrencyPair(
    //        CurrencyPair.of(baseCurrency, _outputCurrency.get()));
    //
    //    final Currency baseCurrency = pair.getBase();
    //    final double exposure = mca.getAmount(pair.getCounter());
    //    final LocalDateDoubleTimeSeries conversionSeries = conversionSeriesResult.getResult();
    //
    //    if (conversionSeriesResult.isResultAvailable()) {
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

    if (calculatorResult.isResultAvailable()) {

      final MultipleCurrencyAmount mca = calculatorResult.getResult().calculateCurrencyExposure();

      if (cpResult.isResultAvailable()) {

        CurrencyPair currencyPair = cpResult.getResult();

        FunctionResult<LocalDateDoubleTimeSeries> returnSeriesResult = _fxReturnSeriesProvider.calculateReturnSeries(
            _seriesPeriod,
            currencyPair);
        final LocalDateDoubleTimeSeries fxSpotReturnSeries = returnSeriesResult.getResult();

        final Currency baseCurrency = currencyPair.getBase();
        final double exposure = mca.getAmount(currencyPair.getCounter());

        if (conversionIsRequired(baseCurrency)) {

          FunctionResult<LocalDateDoubleTimeSeries> conversionSeriesResult = _historicalTimeSeriesProvider.getHtsForCurrencyPair(
              CurrencyPair.of(baseCurrency, _outputCurrency.get()));
          final LocalDateDoubleTimeSeries conversionSeries = conversionSeriesResult.getResult();

          if (conversionSeriesResult.isResultAvailable()) {

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

  private boolean conversionIsRequired(Currency baseCurrency) {

    // No output currency property or it's the same as base 
    return _outputCurrency.or(baseCurrency) != baseCurrency;
  }
}
