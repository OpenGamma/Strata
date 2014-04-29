/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

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
import com.opengamma.financial.security.future.DeliverableSwapFutureSecurity;
import com.opengamma.financial.security.future.FederalFundsFutureSecurity;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.financial.security.irs.FloatingInterestRateSwapLeg;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.financial.security.option.BondFutureOptionSecurity;
import com.opengamma.financial.security.option.IRFutureOptionSecurity;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.id.ExternalId;
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
  private static final Period ONE_MONTH = Period.ofMonths(1);

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

  private Result<HistoricalTimeSeries> processResult(ExternalIdBundle id, HistoricalTimeSeries timeSeries) {
    if (timeSeries != null) {
      if (timeSeries.getTimeSeries().isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "Time series for {} is empty", id);
      } else {
        return Result.success(timeSeries);
      }
    } else {
      return Result.failure(FailureStatus.MISSING_DATA, "Couldn't get time series for {}", id);
    }
  }

  @Override
  public Result<HistoricalTimeSeriesBundle> getFixingsForSecurity(Environment env, FinancialSecurity security) {
    final FixingRetriever retriever = new FixingRetriever(_htsSource, env);
    try {
      return security.accept(retriever);
    } catch (Exception ex) {
      return Result.failure(ex);
    }
  }

  /**
   * Class that returns a timeseries bundle of the fixing timeseries required by a security.
   */
  private class FixingRetriever extends FinancialSecurityVisitorAdapter<Result<HistoricalTimeSeriesBundle>> {

    /**
     * HTSSource to obtain series from.
     */
    private final HistoricalTimeSeriesSource _htsSource;

    /**
     * end date of the fixing series required
     */
    private final LocalDate _valuationDate;

    /**
     * @param htsSource the historical timeseries source
     * @param env the environment
     */
    private FixingRetriever(HistoricalTimeSeriesSource htsSource, Environment env) {
      _htsSource = htsSource;
      _valuationDate = env.getValuationDate();
    }

    /**
     * Returns a time series bundle of the previous month's market values for the specified security.
     * @param dataField the data field, usually Market_Value
     * @param period the period of time to return data for
     * @param ids the externalIdBundles to get series for
     * @return a historical time series bundle
     */
    private Result<HistoricalTimeSeriesBundle> getTimeSeriesBundle(String dataField, Period period, ExternalIdBundle... ids) {
      final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();
      Result<?> result = Result.success(true);
      for (ExternalIdBundle id : ids) {
        Result<HistoricalTimeSeries> series = getPreviousPeriodValues(dataField, id, period);
        if (series.isSuccess()) {
          bundle.add(dataField, id, series.getValue());
        } else {
          result = Result.failure(result, series);
        }
      }
      if (Result.allSuccessful(result)) {
        return Result.success(bundle);
      }
      return Result.failure(result);
    }

    /**
     * Returns a time series bundle of the previous month's market values for the specified security.
     * @param security the security to retrieve the market values for.
     */
    private Result<HistoricalTimeSeries> getMarketValueTimeSeries(FinancialSecurity security) {
      String field = MarketDataRequirementNames.MARKET_VALUE;
      ExternalIdBundle id = security.getExternalIdBundle();
      return getPreviousMonthValues(field, id);
    }

    /**
     * Returns a time series of the previous month's field values for the specified external id into the time series bundle.
     * @param field the name of the value used to lookup.
     * @param id the external id of used to lookup the field values.
     * @return the time series result
     */
    private Result<HistoricalTimeSeries> getPreviousMonthValues(String field, ExternalIdBundle id) {
      return getPreviousPeriodValues(field, id, ONE_MONTH);
    }

    /**
     * Returns a time series of the previous month's field values for the specified external id into the time series bundle.
     * @param field the name of the value used to lookup.
     * @param id the external id of used to lookup the field values.
     * @param length the length of time to get values for.
     * @return the time series result
     */
    private Result<HistoricalTimeSeries> getPreviousPeriodValues(String field, ExternalIdBundle id, Period length) {
      final boolean includeStart = true;
      final boolean includeEnd = true;
      final LocalDate startDate = _valuationDate.minus(length);
      HistoricalTimeSeries series = _htsSource.getHistoricalTimeSeries(field, id, _resolutionKey, startDate, includeStart, _valuationDate, includeEnd);
      return processResult(id, series);
    }

    @Override
    public Result<HistoricalTimeSeriesBundle> visitFederalFundsFutureSecurity(FederalFundsFutureSecurity security) {
      return getTimeSeriesBundle(MarketDataRequirementNames.MARKET_VALUE, ONE_MONTH,
                                 security.getExternalIdBundle(), security.getUnderlyingId().toBundle());
    }
    
    @Override
    public Result<HistoricalTimeSeriesBundle> visitInterestRateFutureSecurity(InterestRateFutureSecurity security) {
      return getTimeSeriesBundle(MarketDataRequirementNames.MARKET_VALUE, ONE_MONTH, security.getExternalIdBundle());
    }
    
    @Override
    public Result<HistoricalTimeSeriesBundle> visitIRFutureOptionSecurity(IRFutureOptionSecurity security) {
      return getTimeSeriesBundle(MarketDataRequirementNames.MARKET_VALUE, ONE_MONTH, security.getExternalIdBundle());
    }

    @Override
    public Result<HistoricalTimeSeriesBundle> visitSwaptionSecurity(SwaptionSecurity security) {
      if (security.getCurrency().equals(Currency.BRL)) {
        throw new UnsupportedOperationException("Fixing series for Brazilian swaptions not yet implemented");
      }
      return Result.success(EMPTY_TIME_SERIES_BUNDLE);
    }

    @Override
    public Result<HistoricalTimeSeriesBundle> visitBondFutureSecurity(BondFutureSecurity security) {
      return getTimeSeriesBundle(MarketDataRequirementNames.MARKET_VALUE, ONE_MONTH, security.getExternalIdBundle());
    }
    
    @Override
    public Result<HistoricalTimeSeriesBundle> visitDeliverableSwapFutureSecurity(DeliverableSwapFutureSecurity security) {
      return getTimeSeriesBundle(MarketDataRequirementNames.MARKET_VALUE, ONE_MONTH, security.getExternalIdBundle());
    }   
    
    @Override
    public Result<HistoricalTimeSeriesBundle> visitBondFutureOptionSecurity(BondFutureOptionSecurity security) {
      return getTimeSeriesBundle(MarketDataRequirementNames.MARKET_VALUE, ONE_MONTH, security.getExternalIdBundle());
    }

    @Override
    public Result<HistoricalTimeSeriesBundle> visitInterestRateSwapSecurity(final InterestRateSwapSecurity security) {
      Collection<ExternalIdBundle> ids = new ArrayList<>();
      for (final FloatingInterestRateSwapLeg leg : security.getLegs(FloatingInterestRateSwapLeg.class)) {
        ExternalId id = leg.getFloatingReferenceRateId();
        ids.add(id.toBundle());
      }
      return getTimeSeriesBundle(MarketDataRequirementNames.MARKET_VALUE, Period.ofYears(1), ids.toArray(new ExternalIdBundle[ids.size()]));
    }

  }

  @Override
  public Result<HistoricalTimeSeriesBundle> getHtsForCurveNode(Environment env, CurveNodeWithIdentifier node, LocalDateRange dateRange) {
    LocalDate startDate = dateRange.getStartDateInclusive();
    LocalDate endDate = dateRange.getEndDateInclusive();
    final boolean includeStart = true;
    final boolean includeEnd = true;

    Result<?> bundleResult = Result.success(true);
    final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();
    
    ExternalIdBundle id = ExternalIdBundle.of(node.getIdentifier());
    String dataField = node.getDataField();
    // TODO use HistoricalMarketDataFn.getValues()?
    HistoricalTimeSeries timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id, _resolutionKey, startDate, includeStart, endDate, includeEnd);
    Result<HistoricalTimeSeries> timeSeriesResult = processResult(id, timeSeries);
    if (timeSeriesResult.isSuccess()) {
      bundle.add(dataField, id, timeSeries);
    } else {
      bundleResult = Result.failure(timeSeriesResult, bundleResult);
    }

    if (node instanceof PointsCurveNodeWithIdentifier) {
      final PointsCurveNodeWithIdentifier pointsNode = (PointsCurveNodeWithIdentifier) node;
      id = ExternalIdBundle.of(pointsNode.getUnderlyingIdentifier());
      dataField = pointsNode.getUnderlyingDataField();
      timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id, _resolutionKey, startDate, includeStart, endDate, includeEnd);
      timeSeriesResult = processResult(id, timeSeries);
      if (timeSeriesResult.isSuccess()) {
        bundle.add(dataField, id, timeSeries);
      } else {
        bundleResult = Result.failure(timeSeriesResult, bundleResult);
      }

    }

    if (node.getCurveNode() instanceof ZeroCouponInflationNode) {
      final ZeroCouponInflationNode inflationNode = (ZeroCouponInflationNode) node.getCurveNode();
      InflationLegConvention inflationLegConvention = _conventionSource.getSingle(inflationNode.getInflationLegConvention(),
                                                                                  InflationLegConvention.class);
      PriceIndexConvention priceIndexConvention = _conventionSource.getSingle(inflationLegConvention.getPriceIndexConvention(),
                                                                              PriceIndexConvention.class);
      final String priceIndexField = MarketDataRequirementNames.MARKET_VALUE; //TODO
      final ExternalIdBundle priceIndexId = ExternalIdBundle.of(priceIndexConvention.getPriceIndexId());
      final HistoricalTimeSeries priceIndexSeries = _htsSource.getHistoricalTimeSeries(priceIndexField, priceIndexId, _resolutionKey,
                                                                                       startDate, includeStart, endDate, includeEnd);
      timeSeriesResult = processResult(id, priceIndexSeries);
      if (timeSeriesResult.isSuccess()) {
        bundle.add(dataField, id, priceIndexSeries);
      } else {
        bundleResult = Result.failure(timeSeriesResult, bundleResult);
      }
    }
    
    if (Result.anyFailures(bundleResult)) {
      return Result.failure(bundleResult);
    }
    
    return Result.success(bundle);  
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(Environment env, CurrencyPair currencyPair, LocalDateRange dateRange) {
    return _historicalMarketDataFn.getFxRates(env, currencyPair, dateRange);
  }
}
