/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.google.common.collect.Lists;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.ZeroCouponInflationNode;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.convention.InflationLegConvention;
import com.opengamma.financial.convention.PriceIndexConvention;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.FinancialSecurityVisitorAdapter;
import com.opengamma.financial.security.future.BondFutureSecurity;
import com.opengamma.financial.security.future.FederalFundsFutureSecurity;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Function implementation that provides a historical time-series bundle.
 */
public class DefaultHistoricalTimeSeriesFn implements HistoricalTimeSeriesFn {

  private static final Logger s_logger = LoggerFactory.getLogger(DefaultHistoricalTimeSeriesFn.class);

  private static final HistoricalTimeSeriesBundle EMPTY_TIME_SERIES_BUNDLE = new HistoricalTimeSeriesBundle();

  private final HistoricalTimeSeriesSource _htsSource;
  private final String _resolutionKey;
  private final ConventionSource _conventionSource;
  private final Period _htsRetrievalPeriod;
  private final HistoricalMarketDataFn _historicalMarketDataFn;

  public DefaultHistoricalTimeSeriesFn(HistoricalTimeSeriesSource htsSource,
                                       String resolutionKey,
                                       ConventionSource conventionSource,
                                       HistoricalMarketDataFn historicalMarketDataFn,
                                       RetrievalPeriod htsRetrievalPeriod) {
    _htsSource = htsSource;
    _resolutionKey = resolutionKey;
    _conventionSource = conventionSource;
    _htsRetrievalPeriod = htsRetrievalPeriod.getRetrievalPeriod();
    _historicalMarketDataFn = historicalMarketDataFn;
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(Environment env, CurrencyPair currencyPair, LocalDate endDate) {
    LocalDate startDate = endDate.minus(_htsRetrievalPeriod);
    LocalDateRange dateRange = LocalDateRange.of(startDate, endDate, true);
    return getHtsForCurrencyPair(env, currencyPair, dateRange);
  }


  public Result<HistoricalTimeSeriesBundle> getHtsForCurveNode(Environment env, CurveNodeWithIdentifier node, LocalDate endDate) {
    // For expediency we will mirror the current ways of working out dates which is
    // pretty much to take 1 year before the valuation date. This is blunt and
    // returns more data than is actually required
    // todo - could we manage HTS lookup in the same way as market data? i.e. request the values needed look them up so they are available next time
    
    final LocalDate startDate = endDate.minus(_htsRetrievalPeriod);
    return getHtsForCurveNode(env, node, LocalDateRange.of(startDate, endDate, true));

  }

  private void processResult(ExternalIdBundle id, String dataField, HistoricalTimeSeries timeSeries, final HistoricalTimeSeriesBundle bundle, List<Result<?>> failures) {
    if (timeSeries != null) {
      if (timeSeries.getTimeSeries().isEmpty()) {
        failures.add(Result.failure(FailureStatus.MISSING_DATA, "Time series for {} is empty", id));
      } else {
        bundle.add(dataField, id, timeSeries);
      }
    } else {
      failures.add(Result.failure(FailureStatus.MISSING_DATA, "Couldn't get time series for {}", id));
    }
  }

  @Override
  public Result<HistoricalTimeSeriesBundle> getFixingsForSecurity(Environment env, FinancialSecurity security) {
    HistoricalTimeSeriesBundle bundle;
    try {
      bundle = security.accept(new FixingRetriever(_htsSource, env));
    } catch (UnsupportedOperationException ex) {
      return Result.failure(ex);
    }
    return Result.success(bundle);
  }
  
  private class FixingRetriever extends FinancialSecurityVisitorAdapter<HistoricalTimeSeriesBundle> {

    private final HistoricalTimeSeriesSource _htsSource;
    
    private final LocalDate _now;
    
    public FixingRetriever(HistoricalTimeSeriesSource htsSource,
                           Environment env) {
      _htsSource = htsSource;
      _now = env.getValuationDate();
    }
    
    @Override
    public HistoricalTimeSeriesBundle visitFederalFundsFutureSecurity(FederalFundsFutureSecurity security) {
      final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();
      
      final String field = MarketDataRequirementNames.MARKET_VALUE;
      final ExternalIdBundle securityId = security.getExternalIdBundle();
      final boolean includeStart = true;
      final boolean includeEnd = true;
      final LocalDate startDate = _now.minus(Period.ofMonths(1));
      final HistoricalTimeSeries securityTimeSeries = _htsSource.getHistoricalTimeSeries(field,
                                                                                         securityId,
                                                                                         _resolutionKey,
                                                                                         startDate,
                                                                                         includeStart,
                                                                                         _now,
                                                                                         includeEnd);
      bundle.add(field, securityId, securityTimeSeries);
      
      final ExternalIdBundle underlyingId = security.getUnderlyingId().getExternalId().toBundle();
      final HistoricalTimeSeries underlyingTimeSeries = _htsSource.getHistoricalTimeSeries(field,
                                                                                           underlyingId,
                                                                                           _resolutionKey,
                                                                                           startDate,
                                                                                           includeStart,
                                                                                           _now,
                                                                                           includeEnd);
      bundle.add(field, underlyingId, underlyingTimeSeries);
      return bundle;
    }
    
    @Override
    public HistoricalTimeSeriesBundle visitInterestRateFutureSecurity(InterestRateFutureSecurity security) {
      final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();
      
      final String field = MarketDataRequirementNames.MARKET_VALUE;
      final ExternalIdBundle id = security.getExternalIdBundle();
      final boolean includeStart = true;
      final boolean includeEnd = true;
      final LocalDate startDate = _now.minus(Period.ofMonths(1));
      final HistoricalTimeSeries timeSeries = _htsSource.getHistoricalTimeSeries(field,
                                                                                 id,
                                                                                 _resolutionKey,
                                                                                 startDate,
                                                                                 includeStart,
                                                                                 _now,
                                                                                 includeEnd);
      bundle.add(field, id, timeSeries);
      return bundle;
    }

    @Override
    public HistoricalTimeSeriesBundle visitSwaptionSecurity(SwaptionSecurity security) {

      if (security.getCurrency().equals(Currency.BRL)) {
        throw new UnsupportedOperationException("Fixing series for Brazilian swaptions not yet implemented");
      }
      return EMPTY_TIME_SERIES_BUNDLE;
    }

    @Override
    public HistoricalTimeSeriesBundle visitBondFutureSecurity(BondFutureSecurity security) {
      return new HistoricalTimeSeriesBundle();
    }
  }

  @Override
  public Result<HistoricalTimeSeriesBundle> getHtsForCurveNode(Environment env, CurveNodeWithIdentifier node, LocalDateRange dateRange) {
    LocalDate startDate = dateRange.getStartDateInclusive();
    LocalDate endDate = dateRange.getEndDateInclusive();
    final boolean includeStart = true;
    final boolean includeEnd = true;

    
    List<Result<?>> failures = Lists.newArrayList();
    
    final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();
    
    ExternalIdBundle id = ExternalIdBundle.of(node.getIdentifier());
    String dataField = node.getDataField();
    // TODO use HistoricalMarketDataFn.getValues()?
    HistoricalTimeSeries timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id, _resolutionKey, startDate,
                                                                         includeStart, endDate, includeEnd);
    processResult(id, dataField, timeSeries, bundle, failures);

    if (node instanceof PointsCurveNodeWithIdentifier) {
      final PointsCurveNodeWithIdentifier pointsNode = (PointsCurveNodeWithIdentifier) node;
      id = ExternalIdBundle.of(pointsNode.getUnderlyingIdentifier());
      dataField = pointsNode.getUnderlyingDataField();
      timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id, _resolutionKey, startDate, includeStart,
                                                      endDate, includeEnd);
      
      processResult(id, dataField, timeSeries, bundle, failures);

    }

    if (node.getCurveNode() instanceof ZeroCouponInflationNode) {
      final ZeroCouponInflationNode inflationNode = (ZeroCouponInflationNode) node.getCurveNode();
      InflationLegConvention inflationLegConvention = _conventionSource.getSingle(inflationNode.getInflationLegConvention(),
                                                                                  InflationLegConvention.class);
      PriceIndexConvention priceIndexConvention = _conventionSource.getSingle(inflationLegConvention.getPriceIndexConvention(),
                                                                              PriceIndexConvention.class);
      final String priceIndexField = MarketDataRequirementNames.MARKET_VALUE; //TODO
      final ExternalIdBundle priceIndexId = ExternalIdBundle.of(priceIndexConvention.getPriceIndexId());
      final HistoricalTimeSeries priceIndexSeries = _htsSource.getHistoricalTimeSeries(priceIndexField,
                                                                                       priceIndexId,
                                                                                       _resolutionKey,
                                                                                       startDate,
                                                                                       includeStart,
                                                                                       endDate,
                                                                                       includeEnd);
      processResult(priceIndexId, priceIndexField, priceIndexSeries, bundle, failures);
    }
    
    if (Result.anyFailures(failures)) {
      return Result.failure(failures);
    }
    
    return Result.success(bundle);  
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(Environment env, CurrencyPair currencyPair, LocalDateRange dateRange) {
    return _historicalMarketDataFn.getFxRates(env, currencyPair, dateRange);
  }
}
