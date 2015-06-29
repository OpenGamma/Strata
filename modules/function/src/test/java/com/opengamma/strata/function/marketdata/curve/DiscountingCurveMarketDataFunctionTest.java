/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketEnvironment;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.DiscountCurveId;

@Test
public class DiscountingCurveMarketDataFunctionTest {

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("group name");

  /**
   * Tests building a single curve
   */
  public void singleCurve() {
    Curve curve = ConstantNodalCurve.of(Currency.AUD + " Discounting", (double) 1);
    DiscountCurveId curveId = DiscountCurveId.of(Currency.AUD, CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(CURVE_GROUP_NAME);
    CurveGroup curveGroup = CurveGroup.of(
        CurveGroupName.of("curveGroup"),
        ImmutableMap.of(Currency.AUD, curve),
        ImmutableMap.of());
    MarketEnvironment
        marketData = MarketEnvironment.builder(TestHelper.date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    DiscountingCurveMarketDataFunction builder = new DiscountingCurveMarketDataFunction();

    Result<Curve> result = builder.build(curveId, marketData, MarketDataConfig.empty());
    assertThat(result).hasValue(curve);
  }

  /**
   * Tests building multiple curves from the same curve group
   */
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
    MarketEnvironment
        marketData = MarketEnvironment.builder(TestHelper.date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    DiscountingCurveMarketDataFunction builder = new DiscountingCurveMarketDataFunction();

    Result<Curve> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).hasValue(curve1);

    Result<Curve> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).hasValue(curve2);
  }

  /**
   * Tests building curves from multiple curve groups
   */
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

    MarketEnvironment marketData =
        MarketEnvironment.builder(TestHelper.date(2011, 3, 8))
            .addValue(groupId1, curveGroup1)
            .addValue(groupId2, curveGroup2)
            .build();

    DiscountingCurveMarketDataFunction builder = new DiscountingCurveMarketDataFunction();

    Result<Curve> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).hasValue(curve1);

    Result<Curve> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).hasValue(curve2);

    Result<Curve> result3 = builder.build(curveId3, marketData, MarketDataConfig.empty());
    assertThat(result3).hasValue(curve3);

    Result<Curve> result4 = builder.build(curveId4, marketData, MarketDataConfig.empty());
    assertThat(result4).hasValue(curve4);
  }

}
