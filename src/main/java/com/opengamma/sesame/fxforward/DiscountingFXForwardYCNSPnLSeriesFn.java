/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.FunctionResultGenerator.failure;
import static com.opengamma.util.result.FunctionResultGenerator.propagateFailure;
import static com.opengamma.util.result.FunctionResultGenerator.propagateFailures;
import static com.opengamma.util.result.FunctionResultGenerator.success;

import java.util.Set;

import javax.inject.Inject;

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
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.FunctionResult;
import com.opengamma.util.time.Tenor;

public class DiscountingFXForwardYCNSPnLSeriesFn implements FXForwardYCNSPnLSeriesFn {

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

  @Inject
  public DiscountingFXForwardYCNSPnLSeriesFn(final FXForwardCalculatorFn calculatorProvider,
                                             final String curveName,
                                             final boolean payLeg,
                                             final Optional<Currency> outputCurrency,
                                             final FXReturnSeriesFn fxReturnSeriesProvider,
                                             final HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             final CurveSpecificationFn curveSpecificationFunction,
                                             final CurrencyPairsFn currencyPairsFn) {
    _calculatorProvider = calculatorProvider;
    _curveName = curveName;
    _payLeg = payLeg;
    _outputCurrency = outputCurrency;
    _fxReturnSeriesProvider = fxReturnSeriesProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _curveSpecificationFunction = curveSpecificationFunction;
    _currencyPairsFn = currencyPairsFn;
  }

  public DiscountingFXForwardYCNSPnLSeriesFn(final FXForwardCalculatorFn calculatorProvider,
                                             final String curveName,
                                             final boolean payLeg,
                                             final FXReturnSeriesFn fxReturnSeriesProvider,
                                             final HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             final CurveSpecificationFn curveSpecificationFunction,
                                             final CurrencyPairsFn currencyPairsFn) {
    this(calculatorProvider, curveName, payLeg, Optional.<Currency>absent(), fxReturnSeriesProvider,
        historicalTimeSeriesProvider, curveSpecificationFunction, currencyPairsFn);
  }

  @Override
  public FunctionResult<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateYCNSPnlSeries(final FXForwardSecurity security) {

    final Currency payCurrency = security.getPayCurrency();
    final Currency receiveCurrency = security.getReceiveCurrency();
    final Currency curveCurrency = _payLeg ? payCurrency : receiveCurrency;
    final UnorderedCurrencyPair pair = UnorderedCurrencyPair.of(payCurrency, receiveCurrency);

    final FunctionResult<CurrencyPair> cpResult = _currencyPairsFn.getCurrencyPair(pair);
    final FunctionResult<FXForwardCalculator> calculatorResult = _calculatorProvider.generateCalculator(security);
    final FunctionResult<CurveSpecification> curveSpecificationResult =
        _curveSpecificationFunction.getCurveSpecification(_curveName);

    if (calculatorResult.isResultAvailable() && curveSpecificationResult.isResultAvailable() &&
        cpResult.isResultAvailable()) {

      final MultipleCurrencyParameterSensitivity bcs = calculatorResult.getResult().generateBlockCurveSensitivities();
      final CurveSpecification curveSpecification = curveSpecificationResult.getResult();

      final FunctionResult<HistoricalTimeSeriesBundle> curveSeriesBundleResult =
          _historicalTimeSeriesProvider.getHtsForCurve(curveSpecification);

      if (curveSeriesBundleResult.isResultAvailable()) {

        final HistoricalTimeSeriesBundle curveSeriesBundle = curveSeriesBundleResult.getResult();
        final DoubleMatrix1D sensitivities = bcs.getSensitivity(_curveName, curveCurrency);
        final Set<CurveNodeWithIdentifier> nodes = curveSpecification.getNodes();

        final int sensitivitiesSize = sensitivities.getNumberOfElements();
        final int nodesSize = nodes.size();

        if (sensitivitiesSize != nodesSize) {
          return failure(FailureStatus.ERROR,
                         "Unequal number of sensitivities ({}) and curve nodes ({})", sensitivitiesSize, nodesSize);
        }

        LocalDateDoubleTimeSeries conversionSeries = generateConversionSeries(curveCurrency);
        return calculateSeriesForNodes(curveSeriesBundle, sensitivities, nodes, conversionSeries);
      }
      return propagateFailure(curveSeriesBundleResult);
    }
    return propagateFailures(calculatorResult, curveSpecificationResult, cpResult);
  }

  private LocalDateDoubleTimeSeries generateConversionSeries(Currency curveCurrency) {

    if (conversionIsRequired(curveCurrency)) {

      final FunctionResult<LocalDateDoubleTimeSeries> conversionSeriesResult =
          _historicalTimeSeriesProvider.getHtsForCurrencyPair(CurrencyPair.of(curveCurrency, _outputCurrency.get()));

      if (conversionSeriesResult.isResultAvailable()) {
        return conversionSeriesResult.getResult();
      }
      // todo handle the cse where we got no result
    }
    return null;
  }

  private FunctionResult<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateSeriesForNodes(HistoricalTimeSeriesBundle curveSeriesBundle,
                                                                                         DoubleMatrix1D sensitivities,
                                                                                         Set<CurveNodeWithIdentifier> nodes,
                                                                                         LocalDateDoubleTimeSeries fxConversionSeries) {
    final int size = sensitivities.getNumberOfElements();

    final Tenor[] keys = new Tenor[size];
    final Object[] labels = new Object[size];
    final LocalDateDoubleTimeSeries[] values = new LocalDateDoubleTimeSeries[size];

    int i = 0;
    for (final CurveNodeWithIdentifier curveNodeWithId : nodes) {

      final String field = curveNodeWithId.getDataField();
      final ExternalId id = curveNodeWithId.getIdentifier();
      final HistoricalTimeSeries hts = curveSeriesBundle.get(field, id);

      if (hts == null || hts.getTimeSeries().isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "Could not get time series for {} with field {}", id, field);
      }

      final LocalDateDoubleTimeSeries ts = hts.getTimeSeries();
      final LocalDateDoubleTimeSeries returnSeries = calculateConvertedReturnSeries(ts, fxConversionSeries);

      keys[i] = curveNodeWithId.getCurveNode().getResolvedMaturity();
      labels[i] = curveNodeWithId.getCurveNode().getName();
      values[i] = returnSeries.multiply(sensitivities.getEntry(i));
      i++;
    }

    return success(new TenorLabelledLocalDateDoubleTimeSeriesMatrix1D(keys, labels, values));
  }

  private LocalDateDoubleTimeSeries calculateConvertedReturnSeries(LocalDateDoubleTimeSeries ts,
                                                                   LocalDateDoubleTimeSeries conversionSeries) {

    LocalDateDoubleTimeSeries series = conversionSeries != null ? ts.multiply(conversionSeries) : ts;
    return _fxReturnSeriesProvider.calculateReturnSeries(series);
  }

  private boolean conversionIsRequired(final Currency baseCurrency) {

    // No output currency property or it's the same as base
    return _outputCurrency.or(baseCurrency) != baseCurrency;
  }
}
