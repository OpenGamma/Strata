/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.DiscountFactorsId;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

/**
 * Test {@link DiscountFactorsMarketDataFunction}.
 */
@Test
public class DiscountFactorsMarketDataFunctionTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("groupName");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_buildZeroRates() {
    Curve curve = ConstantNodalCurve.of(Curves.zeroRates("AUD Discounting", ACT_ACT_ISDA), 1d);
    DiscountCurveId curveId = DiscountCurveId.of(AUD, CURVE_GROUP_NAME, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(curveId, curve)
        .build();
    DiscountFactorsMarketDataFunction test = new DiscountFactorsMarketDataFunction();

    DiscountFactors expected1 = ZeroRateDiscountFactors.of(AUD, VAL_DATE, curve);

    DiscountFactorsId dfId = DiscountFactorsId.of(AUD, CURVE_GROUP_NAME, FEED);
    MarketDataBox<DiscountFactors> result = test.build(dfId, marketData, MarketDataConfig.empty());
    assertThat(result).isEqualTo(MarketDataBox.ofSingleValue(expected1));
  }

  public void test_buildDiscountFactors() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("AUD Discounting", ACT_ACT_ISDA), 1d);
    DiscountCurveId curveId = DiscountCurveId.of(AUD, CURVE_GROUP_NAME, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(curveId, curve)
        .build();
    DiscountFactorsMarketDataFunction test = new DiscountFactorsMarketDataFunction();

    DiscountFactors expected1 = SimpleDiscountFactors.of(AUD, VAL_DATE, curve);

    DiscountFactorsId dfId = DiscountFactorsId.of(AUD, CURVE_GROUP_NAME, FEED);
    MarketDataBox<DiscountFactors> result = test.build(dfId, marketData, MarketDataConfig.empty());
    assertThat(result).isEqualTo(MarketDataBox.ofSingleValue(expected1));
  }

  public void test_noCurve() {
    MarketEnvironment marketData = MarketEnvironment.empty();
    DiscountFactorsMarketDataFunction test = new DiscountFactorsMarketDataFunction();

    DiscountFactorsId dfId = DiscountFactorsId.of(AUD, CURVE_GROUP_NAME, FEED);
    assertThrows(() -> test.build(dfId, marketData, MarketDataConfig.empty()), IllegalArgumentException.class);
  }

  public void test_unknownCurve() {
    Curve curve = ConstantNodalCurve.of(Curves.prices("AUD Prices"), 1d);
    DiscountCurveId curveId = DiscountCurveId.of(AUD, CURVE_GROUP_NAME, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(curveId, curve)
        .build();
    DiscountFactorsMarketDataFunction test = new DiscountFactorsMarketDataFunction();

    DiscountFactorsId dfId = DiscountFactorsId.of(AUD, CURVE_GROUP_NAME, FEED);
    assertThrows(() -> test.build(dfId, marketData, MarketDataConfig.empty()), IllegalArgumentException.class);
  }

}
