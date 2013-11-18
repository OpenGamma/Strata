/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.StandardResultGenerator.success;

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
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;

public class HistoricalTimeSeriesProvider implements HistoricalTimeSeriesProviderFunction {

  private static final Logger s_logger = LoggerFactory.getLogger(HistoricalTimeSeriesProvider.class);

  private final HistoricalTimeSeriesSource _htsSource;
  private final String _resolutionKey;
  private final ConventionSource _conventionSource;
  private final ValuationTimeProviderFunction _valuationTimeProviderFunction;
  private final Period _htsRetrievalPeriod;

  public HistoricalTimeSeriesProvider(HistoricalTimeSeriesSource htsSource,
                                      String resolutionKey,
                                      ConventionSource conventionSource,
                                      ValuationTimeProviderFunction valuationTimeProviderFunction,
                                      Period htsRetrievalPeriod) {
    _htsSource = htsSource;
    _resolutionKey = resolutionKey;
    _conventionSource = conventionSource;
    _valuationTimeProviderFunction = valuationTimeProviderFunction;
    _htsRetrievalPeriod = htsRetrievalPeriod;
  }

  @Override
  public FunctionResult<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(CurrencyPair currencyPair) {


    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public FunctionResult<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curve) {

    // For expediency we will mirror the current ways of working out dates which is
    // pretty much to take 1 year before the valuation date. This is blunt and
    // returns more data than is actually required
    // todo - could we manage HTS lookup in the same way as market data? i.e. request the values needed look them up so they are available next time

    final LocalDate endDate = _valuationTimeProviderFunction.getLocalDate();
    final LocalDate startDate = endDate.minus(_htsRetrievalPeriod);
    final boolean includeStart = true;
    final boolean includeEnd = true;

    final HistoricalTimeSeriesBundle bundle = new HistoricalTimeSeriesBundle();

    for (final CurveNodeWithIdentifier node : curve.getNodes()) {

      ExternalIdBundle id = ExternalIdBundle.of(node.getIdentifier());
      String dataField = node.getDataField();
      HistoricalTimeSeries timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id,
                                                                           _resolutionKey, startDate, includeStart, endDate, includeEnd);
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
        timeSeries = _htsSource.getHistoricalTimeSeries(dataField, id,
                                                        _resolutionKey, startDate, includeStart, endDate, includeEnd);
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
        final HistoricalTimeSeries priceIndexSeries = _htsSource.getHistoricalTimeSeries(priceIndexField, priceIndexId,
                                                                                         _resolutionKey, startDate, includeStart, endDate, includeEnd);
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
