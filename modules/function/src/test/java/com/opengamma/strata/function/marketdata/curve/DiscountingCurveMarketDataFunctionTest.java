/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static org.mockito.Mockito.mock;
import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.CollectProjectAssertions;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.function.marketdata.MarketDataTestUtils;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.DiscountingCurveId;

@Test
public class DiscountingCurveMarketDataFunctionTest {

  private static final MarketDataConfig MARKET_DATA_CONFIG = mock(MarketDataConfig.class);

  /**
   * Tests building a single curve
   */
  public void singleCurve() {
    CurveGroup curveGroup = MarketDataTestUtils.curveGroup();
    YieldCurve curve = MarketDataTestUtils.discountingCurve(1, Currency.AUD, curveGroup);
    DiscountingCurveId curveId = DiscountingCurveId.of(Currency.AUD, MarketDataTestUtils.CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(MarketDataTestUtils.CURVE_GROUP_NAME);
    BaseMarketData marketData = BaseMarketData.builder(TestHelper.date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    DiscountingCurveMarketDataFunction builder = new DiscountingCurveMarketDataFunction();

    Result<YieldCurve> result = builder.build(curveId, marketData, MARKET_DATA_CONFIG);
    CollectProjectAssertions.assertThat(result).hasValue(curve);
  }

  /**
   * Tests building multiple curves from the same curve group
   */
  public void multipleCurves() {
    CurveGroup curveGroup = MarketDataTestUtils.curveGroup();
    YieldCurve curve1 = MarketDataTestUtils.discountingCurve(1, Currency.AUD, curveGroup);
    YieldCurve curve2 = MarketDataTestUtils.discountingCurve(2, Currency.GBP, curveGroup);
    DiscountingCurveId curveId1 = DiscountingCurveId.of(Currency.AUD, MarketDataTestUtils.CURVE_GROUP_NAME);
    DiscountingCurveId curveId2 = DiscountingCurveId.of(Currency.GBP, MarketDataTestUtils.CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(MarketDataTestUtils.CURVE_GROUP_NAME);
    BaseMarketData marketData = BaseMarketData.builder(TestHelper.date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    DiscountingCurveMarketDataFunction builder = new DiscountingCurveMarketDataFunction();

    Result<YieldCurve> result1 = builder.build(curveId1, marketData, MARKET_DATA_CONFIG);
    CollectProjectAssertions.assertThat(result1).hasValue(curve1);

    Result<YieldCurve> result2 = builder.build(curveId2, marketData, MARKET_DATA_CONFIG);
    CollectProjectAssertions.assertThat(result2).hasValue(curve2);
  }

  /**
   * Tests building curves from multiple curve groups
   */
  public void multipleBundles() {
    String groupName1 = "group 1";
    CurveGroup curveGroup1 = MarketDataTestUtils.curveGroup();
    YieldCurve curve1 = MarketDataTestUtils.discountingCurve(1, Currency.AUD, curveGroup1);
    YieldCurve curve2 = MarketDataTestUtils.discountingCurve(2, Currency.GBP, curveGroup1);
    DiscountingCurveId curveId1 = DiscountingCurveId.of(Currency.AUD, groupName1);
    DiscountingCurveId curveId2 = DiscountingCurveId.of(Currency.GBP, groupName1);
    CurveGroupId groupId1 = CurveGroupId.of(groupName1);

    String groupName2 = "group 2";
    CurveGroup curveGroup2 = MarketDataTestUtils.curveGroup();
    YieldCurve curve3 = MarketDataTestUtils.discountingCurve(3, Currency.CHF, curveGroup2);
    YieldCurve curve4 = MarketDataTestUtils.discountingCurve(4, Currency.USD, curveGroup2);
    DiscountingCurveId curveId3 = DiscountingCurveId.of(Currency.CHF, groupName2);
    DiscountingCurveId curveId4 = DiscountingCurveId.of(Currency.USD, groupName2);
    CurveGroupId groupId2 = CurveGroupId.of(groupName2);

    BaseMarketData marketData =
        BaseMarketData.builder(TestHelper.date(2011, 3, 8))
            .addValue(groupId1, curveGroup1)
            .addValue(groupId2, curveGroup2)
            .build();

    DiscountingCurveMarketDataFunction builder = new DiscountingCurveMarketDataFunction();

    Result<YieldCurve> result1 = builder.build(curveId1, marketData, MARKET_DATA_CONFIG);
    CollectProjectAssertions.assertThat(result1).hasValue(curve1);

    Result<YieldCurve> result2 = builder.build(curveId2, marketData, MARKET_DATA_CONFIG);
    CollectProjectAssertions.assertThat(result2).hasValue(curve2);

    Result<YieldCurve> result3 = builder.build(curveId3, marketData, MARKET_DATA_CONFIG);
    CollectProjectAssertions.assertThat(result3).hasValue(curve3);

    Result<YieldCurve> result4 = builder.build(curveId4, marketData, MARKET_DATA_CONFIG);
    CollectProjectAssertions.assertThat(result4).hasValue(curve4);
  }

}
