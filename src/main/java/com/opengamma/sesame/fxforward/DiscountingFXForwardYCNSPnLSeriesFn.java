/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.FunctionResultGenerator.failure;
import static com.opengamma.util.result.FunctionResultGenerator.propagateFailure;
import static com.opengamma.util.result.FunctionResultGenerator.success;

import java.util.Set;

import javax.inject.Inject;

import org.threeten.bp.Period;

import com.google.common.base.Optional;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.financial.analytics.TenorLabelledLocalDateDoubleTimeSeriesMatrix1D;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.FXReturnSeriesFn;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.FunctionResult;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.time.Tenor;

public class DiscountingFXForwardYCNSPnLSeriesFn implements FXForwardPnLSeriesFn<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> {

  private final FXForwardCalculatorFn _calculatorProvider;

  private final String _curveName;
  private final boolean _payLeg;

  /**
   * The requested currency for this P&L series. If not supplied, then the
   * output will be in the base currency of the currency pair corresponding
   * to the FX Forward's currencies.
   */
  private final Optional<Currency> _outputCurrency;

  private final FXReturnSeriesFn _fxReturnSeriesProvider;
  private final HistoricalTimeSeriesFn _historicalTimeSeriesProvider;
  private final CurveSpecificationFn _curveSpecificationFunction;
  private final CurrencyPairsFn _currencyPairsFn;

  /**
   * How big a timeseries result is required. Start date will be valuation date - period.
   */
  private final Period _seriesPeriod;

  @Inject
  public DiscountingFXForwardYCNSPnLSeriesFn(final FXForwardCalculatorFn calculatorProvider,
                                             final String curveName,
                                             final boolean payLeg,
                                             final Optional<Currency> outputCurrency,
                                             final FXReturnSeriesFn fxReturnSeriesProvider,
                                             final HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             final CurveSpecificationFn curveSpecificationFunction,
                                             CurrencyPairsFn currencyPairsFn, final Period seriesPeriod) {
    _calculatorProvider = calculatorProvider;
    _curveName = curveName;
    _payLeg = payLeg;
    _outputCurrency = outputCurrency;
    _fxReturnSeriesProvider = fxReturnSeriesProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _curveSpecificationFunction = curveSpecificationFunction;
    _currencyPairsFn = currencyPairsFn;
    _seriesPeriod = seriesPeriod;
  }

  public DiscountingFXForwardYCNSPnLSeriesFn(final FXForwardCalculatorFn calculatorProvider,
                                             final String curveName,
                                             final boolean payLeg,
                                             final FXReturnSeriesFn fxReturnSeriesProvider,
                                             final HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             final CurveSpecificationFn curveSpecificationFunction,
                                             final Period seriesPeriod, CurrencyPairsFn currencyPairsFn) {
    this(calculatorProvider, curveName, payLeg, Optional.<Currency>absent(), fxReturnSeriesProvider,
        historicalTimeSeriesProvider, curveSpecificationFunction, currencyPairsFn, seriesPeriod);
  }

  @Override
  public FunctionResult<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculatePnlSeries(final FXForwardSecurity security) {
    Currency payCurrency = security.getPayCurrency();
    Currency receiveCurrency = security.getReceiveCurrency();
    final Currency curveCurrency = _payLeg ? payCurrency : receiveCurrency;
    boolean conversionRequired = conversionIsRequired(curveCurrency);


    final UnorderedCurrencyPair pair = UnorderedCurrencyPair.of(payCurrency, receiveCurrency);
    final FunctionResult<CurrencyPair> cpResult = _currencyPairsFn.getCurrencyPair(pair);

    final FunctionResult<FXForwardCalculator> calculatorResult = _calculatorProvider.generateCalculator(security);
    final FunctionResult<CurveSpecification> curveSpecificationResult = _curveSpecificationFunction.getCurveSpecification(
        _curveName);
    if (calculatorResult.isResultAvailable()) {
      FXForwardCalculator forwardCalculator = calculatorResult.getResult();
      MultipleCurrencyAmount currencyExposure = forwardCalculator.calculateCurrencyExposure();

      final MultipleCurrencyParameterSensitivity bcs = forwardCalculator.generateBlockCurveSensitivities();
      if (curveSpecificationResult.isResultAvailable()) {
        final CurveSpecification curveSpecification = curveSpecificationResult.getResult();
        final FunctionResult<HistoricalTimeSeriesBundle> curveSeriesBundleResult = _historicalTimeSeriesProvider.getHtsForCurve(
            curveSpecification);
        if (curveSeriesBundleResult.isResultAvailable()) {
          final HistoricalTimeSeriesBundle curveSeriesBundle = curveSeriesBundleResult.getResult();
          final DoubleMatrix1D sensitivities = bcs.getSensitivity(_curveName, curveCurrency);
          final Set<CurveNodeWithIdentifier> nodes = curveSpecification.getNodes();
          final int n = sensitivities.getNumberOfElements();
          if (n != nodes.size()) {
            return failure(FailureStatus.ERROR, "Unequal number of sensitivities ({}) and curve nodes ({})", n, nodes.size());
          }
          final Tenor[] keys = new Tenor[n];
          final Object[] labels = new Object[n];
          final LocalDateDoubleTimeSeries[] values = new LocalDateDoubleTimeSeries[n];

          if (cpResult.isResultAvailable()) {
            final double exposure = currencyExposure.getAmount(cpResult.getResult().getCounter());

             LocalDateDoubleTimeSeries convertedSeries = null;
            if (conversionRequired) {

              final FunctionResult<LocalDateDoubleTimeSeries> conversionSeriesResult = _historicalTimeSeriesProvider.getHtsForCurrencyPair(
                  CurrencyPair.of(curveCurrency, _outputCurrency.get()));

              if (conversionSeriesResult.isResultAvailable()) {
                final LocalDateDoubleTimeSeries conversionSeries = conversionSeriesResult.getResult();
                convertedSeries = conversionSeries.multiply(exposure);

              }
            }


          int i = 0;
          for (final CurveNodeWithIdentifier curveNodeWithId : nodes) {
            final String field = curveNodeWithId.getDataField();
            final ExternalId id = curveNodeWithId.getIdentifier();
            final HistoricalTimeSeries hts = curveSeriesBundle.get(field, id);
            if (hts == null || hts.getTimeSeries().isEmpty()) {
              return failure(FailureStatus.MISSING_DATA, "Could not get time series for {} with field {}", id, field);
            }

            final LocalDateDoubleTimeSeries ts = hts.getTimeSeries();
            // TODO return series calculation
            // TODO need to multiply by the FX conversion series if required before the return series is calculated

            final LocalDateDoubleTimeSeries returnSeries;
            if (conversionRequired) {
              final LocalDateDoubleTimeSeries converted = ts.multiply(convertedSeries);
              returnSeries = _fxReturnSeriesProvider.calculateReturnSeries(converted);
            } else {
              returnSeries = _fxReturnSeriesProvider.calculateReturnSeries(ts);
            }



            keys[i] = curveNodeWithId.getCurveNode().getResolvedMaturity();
            labels[i] = curveNodeWithId.getCurveNode().getName();
            values[i] = returnSeries.multiply(sensitivities.getEntry(i));
            i++;
          }
          return success(new TenorLabelledLocalDateDoubleTimeSeriesMatrix1D(keys, labels, values));
          }
        }
        return propagateFailure(curveSeriesBundleResult);
      }
      return propagateFailure(curveSpecificationResult);
    }
    return propagateFailure(calculatorResult);
  }

  private boolean conversionIsRequired(final Currency baseCurrency) {

    // No output currency property or it's the same as base
    return _outputCurrency.or(baseCurrency) != baseCurrency;
  }
}
