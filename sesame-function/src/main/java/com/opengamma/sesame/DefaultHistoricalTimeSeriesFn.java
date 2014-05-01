/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.ArrayList;
import java.util.Collection;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.opengamma.core.convention.Convention;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.link.ConventionLink;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.RateFutureNode;
import com.opengamma.financial.analytics.ircurve.strips.ZeroCouponInflationNode;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.convention.FederalFundsFutureConvention;
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
@SuppressWarnings("deprecation")
public class DefaultHistoricalTimeSeriesFn implements HistoricalTimeSeriesFn {

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

  @Override
  public Result<HistoricalTimeSeriesBundle> getFixingsForSecurity(Environment env, FinancialSecurity security) {
    final FixingRetriever retriever = new FixingRetriever(env.getValuationDate());
    try {
      return security.accept(retriever);
    } catch (Exception ex) {
      return Result.failure(ex);
    }
  }

  /**
   * Class that returns a timeseries bundle of the fixing timeseries required by a security.
   */
  private final class FixingRetriever extends FinancialSecurityVisitorAdapter<Result<HistoricalTimeSeriesBundle>> {

    /**
     * end date of the fixing series required
     */
    private final LocalDate _valuationDate;

    /**
     * @param valuationDate the valuation date
     */
    private FixingRetriever(LocalDate valuationDate) {
      _valuationDate = valuationDate;
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
      if (result.isSuccess()) {
        return Result.success(bundle);
      }
      return Result.failure(result);
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
      return getHistoricalTimeSeriesResult(field, id, _resolutionKey, startDate, includeStart, _valuationDate, includeEnd);
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
    final String dataFieldDefault = MarketDataRequirementNames.MARKET_VALUE; //TODO The field should be in the config
    Result<?> bundleResult = Result.success(true);
    final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();
    if (node.getCurveNode() instanceof RateFutureNode) {
      /** Fixing series are required for Fed Fund futures: underlying overnight index fixing (when fixing month has started) */
      RateFutureNode nodeRateFut = (RateFutureNode) node.getCurveNode();
      Convention conventionRateFut =  ConventionLink.resolvable(nodeRateFut.getFutureConvention(), Convention.class).resolve();
      if (conventionRateFut instanceof FederalFundsFutureConvention) {
        FederalFundsFutureConvention conventionFedFundFut = (FederalFundsFutureConvention) conventionRateFut;
        final ExternalIdBundle onIndexId = ExternalIdBundle.of(conventionFedFundFut.getIndexConvention());
        final Result<HistoricalTimeSeries> priceIndexSeries = getHistoricalTimeSeriesResult(dataFieldDefault, onIndexId, _resolutionKey,
            startDate, includeStart, endDate, includeEnd);
        if (priceIndexSeries.isSuccess()) {
          bundle.add(dataFieldDefault, onIndexId, priceIndexSeries.getValue());
        } else {
          bundleResult = Result.failure(priceIndexSeries, bundleResult);
        }
      }
    } else if (node instanceof PointsCurveNodeWithIdentifier) {
      final PointsCurveNodeWithIdentifier pointsNode = (PointsCurveNodeWithIdentifier) node;
      ExternalIdBundle id = ExternalIdBundle.of(pointsNode.getUnderlyingIdentifier());
      String dataField = pointsNode.getUnderlyingDataField();
      Result<HistoricalTimeSeries> timeSeriesResult = getHistoricalTimeSeriesResult(dataField, id, _resolutionKey, startDate, includeStart, endDate, includeEnd);
      if (timeSeriesResult.isSuccess()) {
        bundle.add(dataField, id, timeSeriesResult.getValue());
      } else {
        bundleResult = Result.failure(timeSeriesResult, bundleResult);
      }
    }
    /** Fixing series are required for inflation swaps (starting price index) */
    if (node.getCurveNode() instanceof ZeroCouponInflationNode) {
      final ZeroCouponInflationNode inflationNode = (ZeroCouponInflationNode) node.getCurveNode();
      InflationLegConvention inflationLegConvention = _conventionSource.getSingle(inflationNode.getInflationLegConvention(), InflationLegConvention.class);
      PriceIndexConvention priceIndexConvention = _conventionSource.getSingle(inflationLegConvention.getPriceIndexConvention(), PriceIndexConvention.class);
      final ExternalIdBundle priceIndexId = ExternalIdBundle.of(priceIndexConvention.getPriceIndexId());
      final Result<HistoricalTimeSeries> priceIndexResult = getHistoricalTimeSeriesResult(dataFieldDefault, priceIndexId, _resolutionKey, startDate, includeStart, endDate, includeEnd);
      if (priceIndexResult.isSuccess()) {
        bundle.add(dataFieldDefault, priceIndexId, priceIndexResult.getValue());
      } else {
        bundleResult = Result.failure(bundleResult, priceIndexResult);
      }
    }
    /** Fixing series are required for Ibor swaps (when the UseFixing flag is true)  
     *  [PLAT-6430] Add the UseFixing treatment. */
    
    if (!bundleResult.isSuccess()) {
      return Result.failure(bundleResult);
    }
    return Result.success(bundle);  
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(Environment env, CurrencyPair currencyPair, LocalDateRange dateRange) {
    return _historicalMarketDataFn.getFxRates(env, currencyPair, dateRange);
  }

  /**
   * Wraps the timeseries call and return a Result
   * @param field the field
   * @param id the id
   * @param resolutionKey the resolution key
   * @param startDate the start date
   * @param includeStart should include start
   * @param endDate the end date
   * @param includeEnd should include end
   * @return the time series Result
   */
  private Result<HistoricalTimeSeries> getHistoricalTimeSeriesResult(String field, ExternalIdBundle id, String resolutionKey, LocalDate startDate, 
      boolean includeStart, LocalDate endDate, boolean includeEnd) {
    HistoricalTimeSeries series = _htsSource.getHistoricalTimeSeries(field, id, resolutionKey, startDate, includeStart, endDate, includeEnd);
    if (series != null) {
      if (series.getTimeSeries().isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "Time series for {} is empty", id);
      } else {
        return Result.success(series);
      }
    }
    return Result.failure(FailureStatus.MISSING_DATA, "Couldn't get time series for {}", id);
  }
}
