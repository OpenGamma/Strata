/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.instrument.InstrumentDefinitionWithData;
import com.opengamma.analytics.financial.instrument.future.FederalFundsFutureTransactionDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.core.link.ConventionLink;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.DeliverableSwapFutureNode;
import com.opengamma.financial.analytics.ircurve.strips.RateFutureNode;
import com.opengamma.financial.analytics.ircurve.strips.ZeroCouponInflationNode;
import com.opengamma.financial.convention.FederalFundsFutureConvention;
import com.opengamma.financial.convention.InflationLegConvention;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.timeseries.DoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleEntryIterator;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.timeseries.precise.zdt.ImmutableZonedDateTimeDoubleTimeSeries;
import com.opengamma.timeseries.precise.zdt.ZonedDateTimeDoubleTimeSeries;
import com.opengamma.timeseries.precise.zdt.ZonedDateTimeDoubleTimeSeriesBuilder;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Default curve node converter implementation.
 */
public class DefaultCurveNodeConverterFn implements CurveNodeConverterFn {

  private final HistoricalMarketDataFn _historicalMarketDataFn;

  // TODO where should this come from?
  private final Period _timeSeriesDuration;

  // TODO use a real Period when Joda supports it
  public DefaultCurveNodeConverterFn(HistoricalMarketDataFn historicalMarketDataFn, RetrievalPeriod timeSeriesDuration) {
    _timeSeriesDuration = ArgumentChecker.notNull(timeSeriesDuration, "timeSeriesDuration").getRetrievalPeriod();
    _historicalMarketDataFn = ArgumentChecker.notNull(historicalMarketDataFn, "historicalMarketDataFn");
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result<InstrumentDerivative> getDerivative(Environment env,
                                                    CurveNodeWithIdentifier node,
                                                    InstrumentDefinition<?> definition,
                                                    ZonedDateTime valuationTime) {
    ArgumentChecker.notNull(node, "node");
    ArgumentChecker.notNull(definition, "definition");
    ArgumentChecker.notNull(valuationTime, "time");

    if (definition instanceof InstrumentDefinitionWithData<?, ?> && requiresFixingSeries(node.getCurveNode())) {
      String dataField = node.getDataField();

      if (node.getCurveNode() instanceof ZeroCouponInflationNode) {
        ExternalId priceIndexId;
        ZeroCouponInflationNode zeroCouponInflationNode = (ZeroCouponInflationNode) node.getCurveNode();
        ExternalId conventionId = zeroCouponInflationNode.getInflationLegConvention();
        ConventionLink<InflationLegConvention> conventionLink = ConventionLink.resolvable(conventionId, InflationLegConvention.class);
        InflationLegConvention convention = conventionLink.resolve();
        priceIndexId = convention.getPriceIndexConvention();
        LocalDateRange dateRange = LocalDateRange.of(valuationTime.toLocalDate().minus(_timeSeriesDuration), valuationTime.toLocalDate(), true);
        Result<LocalDateDoubleTimeSeries> timeSeriesResult =
            _historicalMarketDataFn.getValues(env, priceIndexId.toBundle(), FieldName.of(dataField), dateRange);

        if (!timeSeriesResult.isSuccess()) {
          return Result.failure(timeSeriesResult);
        }
        LocalDateDoubleTimeSeries timeSeries = timeSeriesResult.getValue();
        // the timeseries is multiply by 100 because Bloomberg do not provide the right one
        ZonedDateTimeDoubleTimeSeries scaledSeries = convertTimeSeries(ZoneId.of("UTC"), timeSeries.multiply(100));
        InstrumentDefinitionWithData<?, ZonedDateTimeDoubleTimeSeries[]> definitionWithData =
            (InstrumentDefinitionWithData<?, ZonedDateTimeDoubleTimeSeries[]>) definition;
        ZonedDateTimeDoubleTimeSeries[] timeSeriesArray = {scaledSeries, scaledSeries};
        InstrumentDerivative derivative = definitionWithData.toDerivative(valuationTime, timeSeriesArray);
        return Result.success(derivative);
      }

      if (definition instanceof FederalFundsFutureTransactionDefinition) {
        RateFutureNode nodeFFF = (RateFutureNode) node.getCurveNode();
        FederalFundsFutureConvention conventionFFF =  ConventionLink.resolvable(nodeFFF.getFutureConvention(),
                                                                                FederalFundsFutureConvention.class).resolve();
        LocalDateRange dateRange = LocalDateRange.of(valuationTime.toLocalDate().minus(_timeSeriesDuration), valuationTime.toLocalDate(), true);
        ExternalIdBundle indexConventionId = conventionFFF.getIndexConvention().toBundle();
        Result<LocalDateDoubleTimeSeries> timeSeriesResult =
            _historicalMarketDataFn.getValues(env, indexConventionId, FieldName.of(dataField), dateRange);

        if (!timeSeriesResult.isSuccess()) {
          return Result.failure(timeSeriesResult);
        }
        ZonedDateTimeDoubleTimeSeries convertedSeries = convertTimeSeries(valuationTime.getZone(), timeSeriesResult.getValue());
        // No time series is passed for the closing price; for curve calibration only the trade price is required.
        InstrumentDefinitionWithData<?, DoubleTimeSeries<ZonedDateTime>[]> definitionInstWithData =
            (InstrumentDefinitionWithData<?, DoubleTimeSeries<ZonedDateTime>[]>) definition;
        return Result.success(definitionInstWithData.toDerivative(valuationTime, new DoubleTimeSeries[]{convertedSeries}));
      }

      if (node.getCurveNode() instanceof RateFutureNode || node.getCurveNode() instanceof DeliverableSwapFutureNode) {
        InstrumentDefinitionWithData<?, Double> definitionWithData = (InstrumentDefinitionWithData<?, Double>) definition;
        return Result.success(definitionWithData.toDerivative(valuationTime, (Double) null));
        // No last closing price is passed; for curve calibration only the trade price is required.
      }
      throw new OpenGammaRuntimeException("Cannot handle swaps with fixings");
    }
    return Result.success(definition.toDerivative(valuationTime));
  }

  public static boolean requiresFixingSeries(CurveNode node) {
    /** Implementation node: fixing series are required for
     - inflation swaps (starting price index)
     - Fed Fund futures: underlying overnight index fixing (when fixing month has started)
     - Ibor swaps (when the UseFixing flag is true)  */
    return node instanceof ZeroCouponInflationNode || node instanceof RateFutureNode;
    // [PLAT-6430] Add case for (SwapNode) node).isUseFixings()
  }

  private static ZonedDateTimeDoubleTimeSeries convertTimeSeries(ZoneId timeZone, LocalDateDoubleTimeSeries localDateTS) {
    // FIXME CASE Converting a daily historical time series to an arbitrary time. Bad idea
    ZonedDateTimeDoubleTimeSeriesBuilder bld = ImmutableZonedDateTimeDoubleTimeSeries.builder(timeZone);
    for (LocalDateDoubleEntryIterator it = localDateTS.iterator(); it.hasNext();) {
      LocalDate date = it.nextTime();
      ZonedDateTime zdt = date.atStartOfDay(timeZone);
      bld.put(zdt, it.currentValueFast());
    }
    return bld.build();
  }
}
