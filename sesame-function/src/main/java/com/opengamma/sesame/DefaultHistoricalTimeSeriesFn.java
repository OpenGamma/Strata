/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.ZeroCouponInflationNode;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.convention.InflationLegConvention;
import com.opengamma.financial.convention.PriceIndexConvention;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataSeries;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.timeseries.date.DateTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Function implementation that provides a historical time-series bundle.
 */
public class DefaultHistoricalTimeSeriesFn implements HistoricalTimeSeriesFn {

  private static final Logger s_logger = LoggerFactory.getLogger(DefaultHistoricalTimeSeriesFn.class);

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
  public Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(CurrencyPair currencyPair, LocalDate endDate) {
    LocalDate startDate = endDate.minus(_htsRetrievalPeriod);
    LocalDateRange dateRange = LocalDateRange.of(startDate, endDate, true);
    MarketDataRequirement requirement = MarketDataRequirementFactory.of(currencyPair);
    Result<MarketDataSeries> result = _historicalMarketDataFn.requestData(requirement, dateRange);
    if (!result.isValueAvailable()) {
      return propagateFailure(result);
    }
    MarketDataSeries series = result.getValue();
    if (series.getStatus(requirement) == MarketDataStatus.AVAILABLE) {
      DateTimeSeries<LocalDate, ?> onlySeries = series.getOnlySeries();
      return success((LocalDateDoubleTimeSeries) onlySeries);
    } else {
      return failure(FailureStatus.MISSING_DATA, "No time series for " + requirement);
    }
  }

  @Override
  public Result<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curve, LocalDate endDate) {
    // For expediency we will mirror the current ways of working out dates which is
    // pretty much to take 1 year before the valuation date. This is blunt and
    // returns more data than is actually required
    // todo - could we manage HTS lookup in the same way as market data? i.e. request the values needed look them up so they are available next time

    final LocalDate startDate = endDate.minus(_htsRetrievalPeriod);
    final boolean includeStart = true;
    final boolean includeEnd = true;

    final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();

    for (final CurveNodeWithIdentifier node : curve.getNodes()) {

      ExternalIdBundle id = ExternalIdBundle.of(node.getIdentifier());
      String dataField = node.getDataField();
      HistoricalTimeSeries timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id, _resolutionKey, startDate,
                                                                           includeStart, endDate, includeEnd);
      if (timeSeries != null) {
        if (timeSeries.getTimeSeries().isEmpty()) {
          s_logger.info("Time series for {} is empty", id);
        } else {
          bundle.add(dataField, id, timeSeries);
        }
      } else {
        s_logger.info("Couldn't get time series for {}", id);
      }

      if (node instanceof PointsCurveNodeWithIdentifier) {
        final PointsCurveNodeWithIdentifier pointsNode = (PointsCurveNodeWithIdentifier) node;
        id = ExternalIdBundle.of(pointsNode.getUnderlyingIdentifier());
        dataField = pointsNode.getUnderlyingDataField();
        timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id, _resolutionKey, startDate, includeStart,
                                                        endDate, includeEnd);
        if (timeSeries != null) {
          if (timeSeries.getTimeSeries().isEmpty()) {
            s_logger.info("Time series for {} is empty", id);
          } else {
            bundle.add(dataField, id, timeSeries);
          }
        } else {
          s_logger.info("Couldn't get time series for {}", id);
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
        final HistoricalTimeSeries priceIndexSeries = _htsSource.getHistoricalTimeSeries(priceIndexField,
                                                                                         priceIndexId,
                                                                                         _resolutionKey,
                                                                                         startDate,
                                                                                         includeStart,
                                                                                         endDate,
                                                                                         includeEnd);
        if (priceIndexSeries != null) {
          if (priceIndexSeries.getTimeSeries().isEmpty()) {
            s_logger.info("Time series for {} is empty", priceIndexId);
          } else {
            bundle.add(dataField, priceIndexId, priceIndexSeries);
          }
        } else {
          s_logger.info("Couldn't get time series for {}", priceIndexId);
        }
      }
    }
    return success(bundle);
  }

}
