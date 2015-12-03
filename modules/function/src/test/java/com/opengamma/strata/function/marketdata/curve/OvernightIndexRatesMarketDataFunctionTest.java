/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.id.IndexRateId;
import com.opengamma.strata.market.id.OvernightIndexRatesId;
import com.opengamma.strata.market.id.RateIndexCurveId;
import com.opengamma.strata.market.value.DiscountOvernightIndexRates;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

/**
 * Test {@link OvernightIndexRatesMarketDataFunction}.
 */
@Test
public class OvernightIndexRatesMarketDataFunctionTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("groupName");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_buildZeroRates() {
    Curve curve = ConstantNodalCurve.of(Curves.zeroRates("USD Discounting", ACT_ACT_ISDA), 1d);
    RateIndexCurveId curveId = RateIndexCurveId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_DATE.minusDays(1), 0.1);
    IndexRateId timeSeriesId = IndexRateId.of(USD_FED_FUND, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(curveId, curve)
        .addTimeSeries(timeSeriesId, timeSeries)
        .build();
    OvernightIndexRatesMarketDataFunction test = new OvernightIndexRatesMarketDataFunction();

    OvernightIndexRates expected1 = DiscountOvernightIndexRates.of(
        USD_FED_FUND, timeSeries, ZeroRateDiscountFactors.of(USD, VAL_DATE, curve));

    OvernightIndexRatesId dfId = OvernightIndexRatesId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    MarketDataBox<OvernightIndexRates> result = test.build(dfId, marketData, MarketDataConfig.empty());
    assertThat(result).isEqualTo(MarketDataBox.ofSingleValue(expected1));
  }

  public void test_buildOvernightIndexRates() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("USD Discounting", ACT_ACT_ISDA), 1d);
    RateIndexCurveId curveId = RateIndexCurveId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_DATE.minusDays(1), 0.1);
    IndexRateId timeSeriesId = IndexRateId.of(USD_FED_FUND, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(curveId, curve)
        .addTimeSeries(timeSeriesId, timeSeries)
        .build();
    OvernightIndexRatesMarketDataFunction test = new OvernightIndexRatesMarketDataFunction();

    OvernightIndexRates expected1 = DiscountOvernightIndexRates.of(
        USD_FED_FUND, timeSeries, SimpleDiscountFactors.of(USD, VAL_DATE, curve));

    OvernightIndexRatesId dfId = OvernightIndexRatesId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    MarketDataBox<OvernightIndexRates> result = test.build(dfId, marketData, MarketDataConfig.empty());
    assertThat(result).isEqualTo(MarketDataBox.ofSingleValue(expected1));
  }

  public void test_noCurve() {
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_DATE.minusDays(1), 0.1);
    IndexRateId timeSeriesId = IndexRateId.of(USD_FED_FUND, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addTimeSeries(timeSeriesId, timeSeries)
        .build();
    OvernightIndexRatesMarketDataFunction test = new OvernightIndexRatesMarketDataFunction();

    OvernightIndexRatesId dfId = OvernightIndexRatesId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    assertThrows(() -> test.build(dfId, marketData, MarketDataConfig.empty()), IllegalArgumentException.class);
  }

  public void test_noTimeSeries() {
    Curve curve = ConstantNodalCurve.of(Curves.zeroRates("USD Discounting", ACT_ACT_ISDA), 1d);
    RateIndexCurveId curveId = RateIndexCurveId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(curveId, curve)
        .build();
    OvernightIndexRatesMarketDataFunction test = new OvernightIndexRatesMarketDataFunction();

    OvernightIndexRates expected = DiscountOvernightIndexRates.of(
        USD_FED_FUND, LocalDateDoubleTimeSeries.empty(), ZeroRateDiscountFactors.of(USD, VAL_DATE, curve));

    OvernightIndexRatesId dfId = OvernightIndexRatesId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    assertThat(test.build(dfId, marketData, MarketDataConfig.empty())).isEqualTo(MarketDataBox.ofSingleValue(expected));
  }

  public void test_unknownCurve() {
    Curve curve = ConstantNodalCurve.of(Curves.prices("USD Prices"), 1d);
    RateIndexCurveId curveId = RateIndexCurveId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_DATE.minusDays(1), 0.1);
    IndexRateId timeSeriesId = IndexRateId.of(USD_FED_FUND, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(curveId, curve)
        .addTimeSeries(timeSeriesId, timeSeries)
        .build();
    OvernightIndexRatesMarketDataFunction test = new OvernightIndexRatesMarketDataFunction();

    OvernightIndexRatesId dfId = OvernightIndexRatesId.of(USD_FED_FUND, CURVE_GROUP_NAME, FEED);
    assertThrows(() -> test.build(dfId, marketData, MarketDataConfig.empty()), IllegalArgumentException.class);
  }

}
