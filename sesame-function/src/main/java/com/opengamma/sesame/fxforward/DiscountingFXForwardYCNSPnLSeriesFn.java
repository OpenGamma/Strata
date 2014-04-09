/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static org.threeten.bp.DayOfWeek.SATURDAY;
import static org.threeten.bp.DayOfWeek.SUNDAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneOffset;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
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
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.FXReturnSeriesFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Triple;

public class DiscountingFXForwardYCNSPnLSeriesFn implements FXForwardYCNSPnLSeriesFn {

  private static final Logger s_logger = LoggerFactory.getLogger(DiscountingFXForwardYCNSPnLSeriesFn.class);
  private static final ImmutableSet<DayOfWeek> s_weekendDays = ImmutableSet.of(SATURDAY, SUNDAY);
  
  
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

  private final FXMatrixFn _fxMatrixFn;
  private final Boolean _useHistoricalSpot;
  
  private final LocalDateRange _dateRange;
  
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
                                             StringSet impliedCurveNames,
                                             DiscountingMulticurveBundleFn discountingMulticurveBundleFn,
                                             FXMatrixFn fxMatrixFn,
                                             Boolean useHistoricalSpot,
                                             LocalDateRange dateRange) {
    _calculatorProvider = calculatorProvider;
    _curveDefinition = curveDefinition;
    _curveConfig = curveConfig;
    _outputCurrency = outputCurrency;
    _fxReturnSeriesProvider = fxReturnSeriesProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _curveSpecificationFunction = curveSpecificationFunction;
    _currencyPairsFn = currencyPairsFn;
    _marketDataFactory = marketDataFactory;
    _impliedCurveNames = impliedCurveNames.getStrings();
    _discountingMulticurveBundleFn = discountingMulticurveBundleFn;
    _fxMatrixFn = fxMatrixFn;
    _useHistoricalSpot = useHistoricalSpot;
    _dateRange = dateRange;
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

    if (calculatorResult.isSuccess() && cpResult.isSuccess()) {

      final MultipleCurrencyParameterSensitivity bcs = calculatorResult.getValue().generateBlockCurveSensitivities(env);
      LocalDate priceSeriesEnd = _dateRange.getEndDateInclusive();
      LocalDate priceSeriesStart = _dateRange.getStartDateInclusive().minusWeeks(1);
      LocalDateRange priceSeriesRange = LocalDateRange.of(priceSeriesStart, priceSeriesEnd, true);
      
      // Generate our version of an HTS Bundle
      ImpliedCurveHtsBundleBuilder builder = new ImpliedCurveHtsBundleBuilder();

      // todo - how do we adjust for holidays?
      for (LocalDate date = priceSeriesStart; !date.isAfter(priceSeriesEnd); date = date.plusDays(1)) {
        
        MarketDataSpecification marketDataSpec = new FixedHistoricalMarketDataSpecification(date);
        MarketDataSource marketDataSource = _marketDataFactory.create(marketDataSpec);

        Environment envForDate = env.with(date.atStartOfDay(ZoneOffset.UTC), marketDataSource);

        // build multicurve for the date
        Result<Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>> result =
            _discountingMulticurveBundleFn.extractImpliedDepositCurveData(envForDate, _curveConfig);

        // TODO consider how to report failures. either log (as here),
        // fail entire calc in all or nothing approach, somewhere in between?
        // [SSM-234]
        if (result.isSuccess()) {
          Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>> resultValue = result.getValue();
          List<Tenor> tenors = resultValue.getFirst();
          List<Double> nodalValues = resultValue.getSecond();

          for (int i = 0; i < tenors.size(); i++) {
            builder.add(date, tenors.get(i), nodalValues.get(i));
          }
        } else {
          //TODO use actual calendars here. [SSM-233]
          if (isWorkingDay(date)) {
            s_logger.warn("Failed to build curve for date {}. Reason: {}", date, result.getFailureMessage());
          }
        }
      }
      
      TenorLabelledLocalDateDoubleTimeSeriesMatrix1D series = builder.toTimeSeries();
      
      final String curveName = _curveDefinition.getName();
      final Map<Currency, DoubleMatrix1D> sensitivities = bcs.getSensitivityByName(curveName);
      if (sensitivities.isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "No sensitivities for curve: {} were found", curveName);
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
        return Result.failure(FailureStatus.ERROR,
                              "Unequal number of sensitivities ({}) and curve tenors ({})", sensitivitySize, tenorsSize);
      }

      LocalDateDoubleTimeSeries conversionSeries = generateConversionSeries(env, curveCurrency, priceSeriesRange);

      final LocalDateDoubleTimeSeries[] values = new LocalDateDoubleTimeSeries[tenorsSize];

      for (int i = 0; i < tenorsSize; i++) {

        Series seriesForTenor = builder.getSeriesForTenor(tenors[i]);
        LocalDateDoubleTimeSeries ts = trimSeries(ImmutableLocalDateDoubleTimeSeries.of(seriesForTenor._dates, seriesForTenor._values));
        final LocalDateDoubleTimeSeries returnSeries = calculateConvertedReturnSeries(env, ts, null);
        LocalDateDoubleTimeSeries pnlSeries = returnSeries.multiply(sensitivity.getEntry(i));
        
        if (!conversionIsRequired(curveCurrency)) {
          values[i] = pnlSeries;
          continue;
        }
        
        //else - do appropriate conversion
        if (_useHistoricalSpot) {
          values[i] = pnlSeries.multiply(conversionSeries.reciprocal());
        } else {
          Currency srcCcy = curveCurrency;
          Currency tgtCcy = _outputCurrency.get();
          FXMatrix fxMatrix = _fxMatrixFn.getFXMatrix(env, Sets.newHashSet(srcCcy, tgtCcy)).getValue();
          double fxRate = fxMatrix.getFxRate(srcCcy, tgtCcy);
          values[i] = pnlSeries.multiply(fxRate);
        }
        
      }

      return Result.success(new TenorLabelledLocalDateDoubleTimeSeriesMatrix1D(tenors, tenors, values));
    } else {
      return Result.failure(calculatorResult, cpResult);
    }
  }


  private boolean isWorkingDay(LocalDate date) {
    //very much a work in progress. should attempt to use
    //local calendar data instead. only used to determine
    //whether to log error messages atm.
    //see [SSM-233] and [SSM-234].
    return !s_weekendDays.contains(date.getDayOfWeek());
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
    LocalDate priceSeriesEnd = _dateRange.getEndDateInclusive();
    //take one week off the start date. this ensures that the start of the underlying
    //price series will provide at least one business day of data before the required start of 
    // the return series. the resulting return series is trimmed so that first day of PnL = 
    //start date.
    LocalDate priceSeriesStart = _dateRange.getStartDateInclusive().minusWeeks(1);
    LocalDateRange priceSeriesRange = LocalDateRange.of(priceSeriesStart, priceSeriesEnd, true);

    if (Result.allSuccessful(calculatorResult, curveSpecificationResult, cpResult)) {

      final MultipleCurrencyParameterSensitivity bcs = calculatorResult.getValue().generateBlockCurveSensitivities(env);
      final CurveSpecification curveSpecification = curveSpecificationResult.getValue();

      // todo - extract common code between this method and calculateForImpliedCurve
      final String curveName = _curveDefinition.getName();
      final Map<Currency, DoubleMatrix1D> sensitivities = bcs.getSensitivityByName(curveName);
      if (sensitivities.isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "No sensitivities for curve: {} were found", curveName);
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
        return Result.failure(FailureStatus.ERROR,
            "Unequal number of sensitivities ({}) and curve nodes ({})",
            sensitivitiesSize,
            nodesSize);
      }

      LocalDateDoubleTimeSeries conversionSeries = generateConversionSeries(env,
          curveCurrency,
          priceSeriesRange);
      //TODO - [SSM-192] may want to use today's spot rate for conversion here
      return calculateSeriesForNodes(env, sensitivity, nodes, conversionSeries, curveName, priceSeriesRange);
    }
    return Result.failure(calculatorResult, curveSpecificationResult, cpResult);
  }

  private LocalDateDoubleTimeSeries generateConversionSeries(Environment env, Currency curveCurrency, LocalDateRange priceSeriesRange) {

    if (conversionIsRequired(curveCurrency)) {

      CurrencyPair currencyPair = CurrencyPair.of(curveCurrency, _outputCurrency.get());
      final Result<LocalDateDoubleTimeSeries> conversionSeriesResult =
          _historicalTimeSeriesProvider.getHtsForCurrencyPair(env, currencyPair, priceSeriesRange);

      if (conversionSeriesResult.isSuccess()) {
        return conversionSeriesResult.getValue();
      }
      // todo handle the case where we got no result
    }
    return null;
  }

  private Result<TenorLabelledLocalDateDoubleTimeSeriesMatrix1D> calculateSeriesForNodes(Environment env,
                                                                                         DoubleMatrix1D sensitivities,
                                                                                         Set<CurveNodeWithIdentifier> nodes,
                                                                                         LocalDateDoubleTimeSeries fxConversionSeries, 
                                                                                         String curveName, 
                                                                                         LocalDateRange priceSeriesRange) {
    final int size = sensitivities.getNumberOfElements();

    final Tenor[] keys = new Tenor[size];
    final Object[] labels = new Object[size];
    final LocalDateDoubleTimeSeries[] values = new LocalDateDoubleTimeSeries[size];

    int i = 0;
    for (final CurveNodeWithIdentifier curveNodeWithId : nodes) {

      final Result<HistoricalTimeSeriesBundle> curveSeriesBundleResult =
          _historicalTimeSeriesProvider.getHtsForCurveNode(env, curveNodeWithId, priceSeriesRange);
      
      if (!curveSeriesBundleResult.isSuccess()) {
        s_logger.error("Failed to resolve HTS for curve node {}. Failure: {}", curveNodeWithId, curveSeriesBundleResult.getFailureMessage());
        continue;
      }
      
      HistoricalTimeSeriesBundle curveSeriesBundle = curveSeriesBundleResult.getValue();
      
      final String field = curveNodeWithId.getDataField();
      final ExternalId id = curveNodeWithId.getIdentifier();
      final HistoricalTimeSeries hts = curveSeriesBundle.get(field, id);

      if (hts == null || hts.getTimeSeries().isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "Could not get time series for {} with field {}", id, field);
      }
      
      LocalDateDoubleTimeSeries ts = trimSeries(hts.getTimeSeries());
      final LocalDateDoubleTimeSeries returnSeries = calculateConvertedReturnSeries(env, ts, fxConversionSeries);

      keys[i] = curveNodeWithId.getCurveNode().getResolvedMaturity();
      String curveNodeName = curveNodeWithId.getCurveNode().getName();
      labels[i] = curveNodeName != null ? curveNodeName : keys[i];
      values[i] = returnSeries.multiply(sensitivities.getEntry(i));
      
      i++;
      
    }

    return Result.success(new TenorLabelledLocalDateDoubleTimeSeriesMatrix1D(keys, labels, values));
  }

  //todo - would be more efficient pass in two lists here rather than the time series.
  //this way, the caller wouldn't be forced to build a ts object to trim the time series.
  private LocalDateDoubleTimeSeries trimSeries(LocalDateDoubleTimeSeries ts) {
    LinkedList<LocalDate> dates = Lists.newLinkedList();
    LinkedList<Double> values = Lists.newLinkedList();
    
    for (int j = ts.size() - 1; j >= 0; j--) {
      LocalDate date = ts.getTimeAtIndex(j);
      Double value = ts.getValueAtIndex(j);
      dates.addFirst(date);
      values.addFirst(value);
      //note - purposely go one date past start series date.
      //this means PnL will start the on (or first available date after)
      //the start date.
      if (date.isBefore(_dateRange.getStartDateInclusive())) {
        break;
      }
    }

    return ImmutableLocalDateDoubleTimeSeries.of(dates, values);
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
