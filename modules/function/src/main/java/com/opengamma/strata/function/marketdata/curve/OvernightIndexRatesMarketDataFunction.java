/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.calc.marketdata.MarketDataLookup;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.id.IndexRateId;
import com.opengamma.strata.market.id.OvernightIndexRatesId;
import com.opengamma.strata.market.id.RateIndexCurveId;
import com.opengamma.strata.market.value.DiscountOvernightIndexRates;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

/**
 * Market data function that builds the provider of Overnight index rates.
 * <p>
 * This function creates an instance of {@link DiscountOvernightIndexRates} based on an underlying curve.
 * The curve may be wrapped in {@link ZeroRateDiscountFactors} or {@link SimpleDiscountFactors}.
 * The type is chosen based on the {@linkplain ValueType value type} held in
 * the {@linkplain CurveMetadata#getYValueType() y-value metadata}.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}.
 * The curve group must be available in the {@code MarketDataLookup} passed to the {@link #build} method.
 */
public class OvernightIndexRatesMarketDataFunction
    implements MarketDataFunction<OvernightIndexRates, OvernightIndexRatesId> {

  @Override
  public MarketDataRequirements requirements(OvernightIndexRatesId id, MarketDataConfig config) {
    RateIndexCurveId curveId = RateIndexCurveId.of(id.getIndex(), id.getCurveGroupName(), id.getMarketDataFeed());
    IndexRateId timeSeriesId = IndexRateId.of(id.getIndex(), id.getMarketDataFeed());
    return MarketDataRequirements.builder()
        .addValues(curveId)
        .addTimeSeries(timeSeriesId)
        .build();
  }

  @Override
  public Result<MarketDataBox<OvernightIndexRates>> build(
      OvernightIndexRatesId id,
      MarketDataLookup marketData,
      MarketDataConfig config) {

    // find time-series
    IndexRateId timeSeriesId = IndexRateId.of(id.getIndex(), id.getMarketDataFeed());
    if (!marketData.containsTimeSeries(timeSeriesId)) {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "No time-series found: Index: {}, Feed: {}",
          id.getIndex(),
          id.getMarketDataFeed());
    }
    LocalDateDoubleTimeSeries timeSeries = marketData.getTimeSeries(timeSeriesId);

    // find curve
    RateIndexCurveId curveId = RateIndexCurveId.of(id.getIndex(), id.getCurveGroupName(), id.getMarketDataFeed());

    if (!marketData.containsValue(curveId)) {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "No curve found: Index: {}, Group: {}, Feed: {}",
          id.getIndex(),
          id.getCurveGroupName(),
          id.getMarketDataFeed());
    }
    MarketDataBox<Curve> curveBox = marketData.getValue(curveId);
    MarketDataBox<LocalDate> valDateBox = marketData.getValuationDate();
    return curveBox.combineWith(valDateBox, (c, valDate) -> createRates(id.getIndex(), valDate, timeSeries, c));
  }

  // create the instance of OvernightIndexRates
  private Result<OvernightIndexRates> createRates(
      OvernightIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries timeSeries,
      Curve curve) {

    ValueType yValueType = curve.getMetadata().getYValueType();
    if (ValueType.ZERO_RATE.equals(yValueType)) {
      ZeroRateDiscountFactors df = ZeroRateDiscountFactors.of(index.getCurrency(), valuationDate, curve);
      return Result.success(DiscountOvernightIndexRates.of(index, timeSeries, df));

    } else if (ValueType.DISCOUNT_FACTOR.equals(yValueType)) {
      SimpleDiscountFactors df = SimpleDiscountFactors.of(index.getCurrency(), valuationDate, curve);
      return Result.success(DiscountOvernightIndexRates.of(index, timeSeries, df));

    } else {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "Invalid curve, must have ValueType of 'ZeroRate' or 'DiscountFactor', but was: {}",
          yValueType);
    }
  }

  @Override
  public Class<OvernightIndexRatesId> getMarketDataIdType() {
    return OvernightIndexRatesId.class;
  }

}
