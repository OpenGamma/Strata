/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.DiscountCurveId;

/**
 * Test {@link DiscountCurveMarketDataFunction}.
 */
@Test
public class DiscountCurveMarketDataFunctionTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("groupName");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  // tests building a single curve
  public void singleCurve() {
    Curve curve = ConstantNodalCurve.of(Currency.AUD + " Discounting", (double) 1);
    DiscountCurveId curveId = DiscountCurveId.of(Currency.AUD, CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(CURVE_GROUP_NAME);
    CurveGroup curveGroup = CurveGroup.of(
        CurveGroupName.of("curveGroup"),
        ImmutableMap.of(Currency.AUD, curve),
        ImmutableMap.of());
    MarketEnvironment marketData = MarketEnvironment.builder().valuationDate(VAL_DATE).addValue(groupId, curveGroup).build();
    DiscountCurveMarketDataFunction builder = new DiscountCurveMarketDataFunction();

    MarketDataBox<Curve> result = builder.build(curveId, marketData, MarketDataConfig.empty());
    assertThat(result).isEqualTo(MarketDataBox.ofSingleValue(curve));
  }

  // tests building multiple curves from the same curve group
  public void multipleCurves() {
    Curve curve1 = ConstantNodalCurve.of(Currency.AUD + " Discounting", (double) 1);
    Curve curve2 = ConstantNodalCurve.of(Currency.GBP + " Discounting", (double) 2);
    DiscountCurveId curveId1 = DiscountCurveId.of(Currency.AUD, CURVE_GROUP_NAME);
    DiscountCurveId curveId2 = DiscountCurveId.of(Currency.GBP, CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(CURVE_GROUP_NAME);
    CurveGroup curveGroup = CurveGroup.of(
        CurveGroupName.of("curveGroup"),
        ImmutableMap.of(Currency.AUD, curve1, Currency.GBP, curve2),
        ImmutableMap.of());
    MarketEnvironment marketData = MarketEnvironment.builder().valuationDate(VAL_DATE).addValue(groupId, curveGroup).build();
    DiscountCurveMarketDataFunction builder = new DiscountCurveMarketDataFunction();

    MarketDataBox<Curve> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).isEqualTo(MarketDataBox.ofSingleValue(curve1));

    MarketDataBox<Curve> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).isEqualTo(MarketDataBox.ofSingleValue(curve2));
  }

  // tests building curves from multiple curve groups
  public void multipleBundles() {
    CurveGroupName groupName1 = CurveGroupName.of("group 1");
    Curve curve1 = ConstantNodalCurve.of(Currency.AUD + " Discounting", (double) 1);
    Curve curve2 = ConstantNodalCurve.of(Currency.GBP + " Discounting", (double) 2);
    DiscountCurveId curveId1 = DiscountCurveId.of(Currency.AUD, groupName1);
    DiscountCurveId curveId2 = DiscountCurveId.of(Currency.GBP, groupName1);
    CurveGroupId groupId1 = CurveGroupId.of(groupName1);
    CurveGroup curveGroup1 = CurveGroup.of(
        CurveGroupName.of("curveGroup"),
        ImmutableMap.of(Currency.AUD, curve1, Currency.GBP, curve2),
        ImmutableMap.of());

    CurveGroupName groupName2 = CurveGroupName.of("group 2");
    Curve curve3 = ConstantNodalCurve.of(Currency.CHF + " Discounting", (double) 3);
    Curve curve4 = ConstantNodalCurve.of(Currency.USD + " Discounting", (double) 4);
    DiscountCurveId curveId3 = DiscountCurveId.of(Currency.CHF, groupName2);
    DiscountCurveId curveId4 = DiscountCurveId.of(Currency.USD, groupName2);
    CurveGroupId groupId2 = CurveGroupId.of(groupName2);
    CurveGroup curveGroup2 = CurveGroup.of(
        CurveGroupName.of("curveGroup"),
        ImmutableMap.of(Currency.CHF, curve3, Currency.USD, curve4),
        ImmutableMap.of());

    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(groupId1, curveGroup1)
        .addValue(groupId2, curveGroup2)
        .build();

    DiscountCurveMarketDataFunction builder = new DiscountCurveMarketDataFunction();

    MarketDataBox<Curve> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).isEqualTo(MarketDataBox.ofSingleValue(curve1));

    MarketDataBox<Curve> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).isEqualTo(MarketDataBox.ofSingleValue(curve2));

    MarketDataBox<Curve> result3 = builder.build(curveId3, marketData, MarketDataConfig.empty());
    assertThat(result3).isEqualTo(MarketDataBox.ofSingleValue(curve3));

    MarketDataBox<Curve> result4 = builder.build(curveId4, marketData, MarketDataConfig.empty());
    assertThat(result4).isEqualTo(MarketDataBox.ofSingleValue(curve4));
  }

  public void test_noCurveGroup() {
    MarketEnvironment marketData = MarketEnvironment.empty();
    DiscountCurveMarketDataFunction test = new DiscountCurveMarketDataFunction();

    DiscountCurveId id = DiscountCurveId.of(AUD, CURVE_GROUP_NAME, FEED);
    assertThrows(() -> test.build(id, marketData, MarketDataConfig.empty()), IllegalArgumentException.class);
  }

  public void test_noCurveOfDesiredCurrencyInGroup() {
    Curve curve = ConstantNodalCurve.of(Currency.GBP + " Discounting", (double) 1);
    CurveGroupId groupId = CurveGroupId.of(CURVE_GROUP_NAME, FEED);
    CurveGroup curveGroup = CurveGroup.of(
        CurveGroupName.of("curveGroup"),
        ImmutableMap.of(Currency.GBP, curve),
        ImmutableMap.of());
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(VAL_DATE)
        .addValue(groupId, curveGroup)
        .build();
    DiscountCurveMarketDataFunction test = new DiscountCurveMarketDataFunction();

    DiscountCurveId id = DiscountCurveId.of(AUD, CURVE_GROUP_NAME, FEED);
    assertThrows(() -> test.build(id, marketData, MarketDataConfig.empty()), IllegalArgumentException.class);
  }

}
