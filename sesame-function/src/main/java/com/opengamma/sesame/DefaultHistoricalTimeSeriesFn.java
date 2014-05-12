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

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
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
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function implementation that provides a historical time-series bundle.
 */
public class DefaultHistoricalTimeSeriesFn implements HistoricalTimeSeriesFn {

  private static final HistoricalTimeSeriesBundle EMPTY_TIME_SERIES_BUNDLE = new HistoricalTimeSeriesBundle();
  private static final Period ONE_MONTH = Period.ofMonths(1);

  private final HistoricalTimeSeriesSource _htsSource;
  private final String _resolutionKey;

  public DefaultHistoricalTimeSeriesFn(HistoricalTimeSeriesSource htsSource, String resolutionKey) {
    _htsSource = htsSource;
    _resolutionKey = resolutionKey;
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
