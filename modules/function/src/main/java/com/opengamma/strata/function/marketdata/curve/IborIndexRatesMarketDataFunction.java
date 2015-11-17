/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.id.IborIndexRatesId;
import com.opengamma.strata.market.id.IndexRateId;
import com.opengamma.strata.market.id.RateIndexCurveId;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.DiscountIborIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

/**
 * Market data function that builds the provider of Ibor index rates.
 * <p>
 * This function creates an instance of {@link DiscountIborIndexRates} based on an underlying curve.
 * The curve may be wrapped in {@link ZeroRateDiscountFactors} or {@link SimpleDiscountFactors}.
 * The type is chosen based on the {@linkplain ValueType value type} held in
 * the {@linkplain CurveMetadata#getYValueType() y-value metadata}.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}.
 * The curve group must be available in the {@code MarketDataLookup} passed to the {@link #build} method.
 */
public class IborIndexRatesMarketDataFunction
    implements MarketDataFunction<IborIndexRates, IborIndexRatesId> {

  @Override
  public MarketDataRequirements requirements(IborIndexRatesId id, MarketDataConfig config) {
    RateIndexCurveId curveId = RateIndexCurveId.of(id.getIndex(), id.getCurveGroupName(), id.getMarketDataFeed());
    IndexRateId timeSeriesId = IndexRateId.of(id.getIndex(), id.getMarketDataFeed());
    return MarketDataRequirements.builder()
        .addValues(curveId)
        .addTimeSeries(timeSeriesId)
        .build();
  }

  @Override
  public MarketDataBox<IborIndexRates> build(
      IborIndexRatesId id,
      CalculationEnvironment marketData,
      MarketDataConfig config) {

    // find time-series
    IborIndex index = id.getIndex();
    IndexRateId timeSeriesId = IndexRateId.of(index, id.getMarketDataFeed());
    LocalDateDoubleTimeSeries timeSeries = marketData.getTimeSeries(timeSeriesId);

    // find curve
    RateIndexCurveId curveId = RateIndexCurveId.of(index, id.getCurveGroupName(), id.getMarketDataFeed());
    MarketDataBox<Curve> curveBox = marketData.getValue(curveId);
    MarketDataBox<LocalDate> valDateBox = marketData.getValuationDate();
    return curveBox.combineWith(valDateBox, (curve, valDate) -> createRates(index, valDate, timeSeries, curve));
  }

  // create the instance of IborIndexRates
  private IborIndexRates createRates(
      IborIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries timeSeries,
      Curve curve) {

    DiscountFactors df = DiscountFactors.of(index.getCurrency(), valuationDate, curve);
    return DiscountIborIndexRates.of(index, timeSeries, df);
  }

  @Override
  public Class<IborIndexRatesId> getMarketDataIdType() {
    return IborIndexRatesId.class;
  }

}
