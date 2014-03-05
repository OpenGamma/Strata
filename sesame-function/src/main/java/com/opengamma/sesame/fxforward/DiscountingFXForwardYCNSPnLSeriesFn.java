/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.propagateFailures;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;

import com.google.common.base.Optional;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.financial.analytics.TenorLabelledLocalDateDoubleTimeSeriesMatrix1D;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.FXReturnSeriesFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Triple;

public class DiscountingFXForwardYCNSPnLSeriesFn implements FXForwardYCNSPnLSeriesFn {

  private static final Logger s_logger = LoggerFactory.getLogger(DiscountingFXForwardYCNSPnLSeriesFn.class);

  private final FXForwardCalculatorFn _calculatorProvider;

  private final CurveDefinition _curveDefinition;
  private final CurveConstructionConfiguration _curveConfig;

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
  private final MarketDataFactory _marketDataFactory;

  // todo - this is only a temporary solution to determine the implied deposit curves
  private final Set<String> _impliedCurveNames;
  private final DiscountingMulticurveBundleFn _discountingMulticurveBundleFn;

  private final Period _seriesPeriod;

  @Inject
  public DiscountingFXForwardYCNSPnLSeriesFn(FXForwardCalculatorFn calculatorProvider,
                                             CurveDefinition curveDefinition,
                                             CurveConstructionConfiguration curveConfig,
                                             Optional<Currency> outputCurrency,
                                             FXReturnSeriesFn fxReturnSeriesProvider,
                                             HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                             CurveSpecificationFn curveSpecificationFunction,
                                             CurrencyPairsFn currencyPairsFn,
                                             MarketDataFactory marketDataFactory,
                                             Set<String> impliedCurveNames,
                                             DiscountingMulticurveBundleFn discountingMulticurveBundleFn,
                                             Period seriesPeriod) {
    _calculatorProvider = calculatorProvider;
    _curveDefinition = curveDefinition;
    _curveConfig = curveConfig;
    _outputCurrency = outputCurrency;
    _fxReturnSeriesProvider = fxReturnSeriesProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _curveSpecificationFunction = curveSpecificationFunction;
    _currencyPairsFn = currencyPairsFn;
    _marketDataFactory = marketDataFactory;
    _impliedCurveNames = impliedCurveNames;
    _discountingMulticurveBundleFn = discountingMulticurveBundleFn;
    _seriesPeriod = seriesPeriod;
  }

  @Override
  public Result<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateYCNSPnlSeries(Environment env,
                                                                                       FXForwardSecurity security) {

    // If this is for an Implied Deposit curve we need to behave differently
    // 1. We need to calculate the multicurve bundle for each day that
    // we're interested in by moving the valuation date.
    // 2. This will have created multiple Implied Deposit curves, we need to
    // iterate across them generating a timeseries bundle from the nodal
    // values (or do this as we're generating them
    // 3. Then use the timeseries bundles as we do for a standard curve

    final Currency payCurrency = security.getPayCurrency();
    final Currency receiveCurrency = security.getReceiveCurrency();
    final UnorderedCurrencyPair pair = UnorderedCurrencyPair.of(payCurrency, receiveCurrency);

    final Result<CurrencyPair> cpResult = _currencyPairsFn.getCurrencyPair(pair);

    // todo - these should probably be separate classes as there is little commonality in the methods
    return _impliedCurveNames.contains(_curveDefinition.getName()) ?
        calculateForImpliedCurve(env, security, cpResult) :
        calculateForNonImpliedCurve(env, security, cpResult);
  }

  private Result<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateForImpliedCurve(Environment env,
                                                                                          FXForwardSecurity security,
                                                                                          Result<CurrencyPair> cpResult) {

    // We need the calculator so we can get the block curve sensitivities
    final Result<FXForwardCalculator> calculatorResult = _calculatorProvider.generateCalculator(env, security);

    if (calculatorResult.isValueAvailable() && cpResult.isValueAvailable()) {

      final MultipleCurrencyParameterSensitivity bcs = calculatorResult.getValue().generateBlockCurveSensitivities(env);

      LocalDate end = env.getValuationDate();
      LocalDate start = end.minus(_seriesPeriod);

      // Generate our version of an HTS Bundle
      ImpliedCurveHtsBundleBuilder builder = new ImpliedCurveHtsBundleBuilder();

      // todo - how do we adjust for holidays?
      for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        MarketDataSpecification marketDataSpec = new FixedHistoricalMarketDataSpecification(date);
        MarketDataSource marketDataSource = _marketDataFactory.create(marketDataSpec);

        Environment envForDate = env.with(date.atStartOfDay(ZoneOffset.UTC), marketDataSource);

        // build multicurve for the date
        Result<Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>> result =
            _discountingMulticurveBundleFn.extractImpliedDepositCurveData(envForDate, _curveConfig);

        if (result.isValueAvailable()) {
          Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>> resultValue = result.getValue();
          List<Tenor> tenors = resultValue.getFirst();
          List<Double> nodalValues = resultValue.getSecond();

          for (int i = 0; i < tenors.size(); i++) {
            builder.add(date, tenors.get(i), nodalValues.get(i));
          }
        }
      }

      TenorLabelledLocalDateDoubleTimeSeriesMatrix1D series = builder.toTimeSeries();

      final String curveName = _curveDefinition.getName();
      final Map<Currency, DoubleMatrix1D> sensitivities = bcs.getSensitivityByName(curveName);
      if (sensitivities.isEmpty()) {
        return failure(FailureStatus.MISSING_DATA, "No sensitivities for curve: {} were found", curveName);
      }

      Map.Entry<Currency, DoubleMatrix1D> match = sensitivities.entrySet().iterator().next();
      DoubleMatrix1D sensitivity = match.getValue();
      Currency curveCurrency = match.getKey();

      if (sensitivities.size() > 1) {
        s_logger.warn("Curve name: {} is used multiple times - using one for currency: {}", curveName, curveCurrency);
      }

      final Tenor[] tenors = series.getKeys();
      final int sensitivitySize = sensitivity.getNumberOfElements();
      final int tenorsSize = tenors.length;

      if (sensitivitySize != tenorsSize) {
        return failure(FailureStatus.ERROR,
                       "Unequal number of sensitivities ({}) and curve tenors ({})", sensitivitySize, tenorsSize);
      }

      LocalDateDoubleTimeSeries conversionSeries = generateConversionSeries(env, curveCurrency, env.getValuationDate());

      final LocalDateDoubleTimeSeries[] values = new LocalDateDoubleTimeSeries[tenorsSize];

      for (int i = 0; i < tenorsSize; i++) {

        Series seriesForTenor = builder.getSeriesForTenor(tenors[i]);
        final LocalDateDoubleTimeSeries ts = ImmutableLocalDateDoubleTimeSeries.of(seriesForTenor._dates, seriesForTenor._values);
        final LocalDateDoubleTimeSeries returnSeries = calculateConvertedReturnSeries(env, ts, conversionSeries);
        values[i] = returnSeries.multiply(sensitivity.getEntry(i));
      }

      return success(new TenorLabelledLocalDateDoubleTimeSeriesMatrix1D(tenors, tenors, values));
    } else {
      return propagateFailures(calculatorResult, cpResult);
    }
  }

  private static class Series {

    private final List<LocalDate> _dates = new ArrayList<>();
    private final List<Double> _values = new ArrayList<>();

    public void add(LocalDate date, Double value) {
      _dates.add(date);
      _values.add(value);
    }
  }

  private static class ImpliedCurveHtsBundleBuilder {

    private final Map<Tenor, Series> _series = new HashMap<>();

    public void add(LocalDate date, Tenor tenor, Double aDouble) {
      getSeriesForTenor(tenor).add(date, aDouble);
    }

    private Series getSeriesForTenor(Tenor tenor) {
      if (_series.containsKey(tenor)) {
        return _series.get(tenor);
      } else {
        Series series = new Series();
        _series.put(tenor, series);
        return series;
      }
    }

    public TenorLabelledLocalDateDoubleTimeSeriesMatrix1D toTimeSeries() {

      ArrayList<Tenor> sortedTenors = new ArrayList<>(_series.keySet());
      Collections.sort(sortedTenors);
      int size = sortedTenors.size();

      Tenor[] tenors = new Tenor[size];
      LocalDateDoubleTimeSeries[] series = new LocalDateDoubleTimeSeries[size];

      for (int i = 0; i < size; i++) {

        Tenor tenor = sortedTenors.get(i);
        tenors[i] = tenor;
        Series tenorSeries = _series.get(tenor);
        series[i] = ImmutableLocalDateDoubleTimeSeries.of(tenorSeries._dates, tenorSeries._values);
      }

      return new TenorLabelledLocalDateDoubleTimeSeriesMatrix1D(tenors, series);
    }
  }


  private Result<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateForNonImpliedCurve(Environment env,
                                                                                             FXForwardSecurity security,
                                                                                             Result<CurrencyPair> cpResult) {
    final Result<FXForwardCalculator> calculatorResult = _calculatorProvider.generateCalculator(env, security);
    final Result<CurveSpecification> curveSpecificationResult =
        _curveSpecificationFunction.getCurveSpecification(env, _curveDefinition);

    if (calculatorResult.isValueAvailable() && curveSpecificationResult.isValueAvailable() &&
        cpResult.isValueAvailable()) {

      final MultipleCurrencyParameterSensitivity bcs = calculatorResult.getValue().generateBlockCurveSensitivities(env);
      final CurveSpecification curveSpecification = curveSpecificationResult.getValue();

      final Result<HistoricalTimeSeriesBundle> curveSeriesBundleResult =
          _historicalTimeSeriesProvider.getHtsForCurve(env, curveSpecification, env.getValuationDate());

      if (curveSeriesBundleResult.isValueAvailable()) {

        final HistoricalTimeSeriesBundle curveSeriesBundle = curveSeriesBundleResult.getValue();

        // todo - extract common code between this method and calculateForImpliedCurve
        final String curveName = _curveDefinition.getName();
        final Map<Currency, DoubleMatrix1D> sensitivities = bcs.getSensitivityByName(curveName);
        if (sensitivities.isEmpty()) {
          return failure(FailureStatus.MISSING_DATA, "No sensitivities for curve: {} were found", curveName);
        }

        Map.Entry<Currency, DoubleMatrix1D> match = sensitivities.entrySet().iterator().next();
        DoubleMatrix1D sensitivity = match.getValue();
        Currency curveCurrency = match.getKey();

        if (sensitivities.size() > 1) {
          s_logger.warn("Curve name: {} is used multiple times - using one for currency: {}", curveName, curveCurrency);
        }

        final Set<CurveNodeWithIdentifier> nodes = curveSpecification.getNodes();

        final int sensitivitiesSize = sensitivity.getNumberOfElements();
        final int nodesSize = nodes.size();

        if (sensitivitiesSize != nodesSize) {
          return failure(FailureStatus.ERROR,
                         "Unequal number of sensitivities ({}) and curve nodes ({})", sensitivitiesSize, nodesSize);
        }

        LocalDateDoubleTimeSeries conversionSeries = generateConversionSeries(env, curveCurrency, env.getValuationDate());
        return calculateSeriesForNodes(env, curveSeriesBundle, sensitivity, nodes, conversionSeries);
      }
      return propagateFailure(curveSeriesBundleResult);
    }
    return propagateFailures(calculatorResult, curveSpecificationResult, cpResult);
  }

  private LocalDateDoubleTimeSeries generateConversionSeries(Environment env, Currency curveCurrency, LocalDate valuationDate) {

    if (conversionIsRequired(curveCurrency)) {

      CurrencyPair currencyPair = CurrencyPair.of(curveCurrency, _outputCurrency.get());
      final Result<LocalDateDoubleTimeSeries> conversionSeriesResult =
          _historicalTimeSeriesProvider.getHtsForCurrencyPair(env, currencyPair, valuationDate);

      if (conversionSeriesResult.isValueAvailable()) {
        return conversionSeriesResult.getValue();
      }
      // todo handle the case where we got no result
    }
    return null;
  }

  private Result<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateSeriesForNodes(Environment env,
                                                                                         HistoricalTimeSeriesBundle curveSeriesBundle,
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
      final LocalDateDoubleTimeSeries returnSeries = calculateConvertedReturnSeries(env, ts, fxConversionSeries);

      keys[i] = curveNodeWithId.getCurveNode().getResolvedMaturity();
      String curveNodeName = curveNodeWithId.getCurveNode().getName();
      labels[i] = curveNodeName != null ? curveNodeName : keys[i];
      values[i] = returnSeries.multiply(sensitivities.getEntry(i));
      i++;
    }

    return success(new TenorLabelledLocalDateDoubleTimeSeriesMatrix1D(keys, labels, values));
  }

  private LocalDateDoubleTimeSeries calculateConvertedReturnSeries(Environment env,
                                                                   LocalDateDoubleTimeSeries ts,
                                                                   LocalDateDoubleTimeSeries conversionSeries) {

    LocalDateDoubleTimeSeries series = conversionSeries != null ? ts.multiply(conversionSeries) : ts;
    return _fxReturnSeriesProvider.calculateReturnSeries(env, series);
  }

  private boolean conversionIsRequired(final Currency baseCurrency) {

    // No output currency property or it's the same as base means we don't need to convert
    return _outputCurrency.or(baseCurrency) != baseCurrency;
  }
}
