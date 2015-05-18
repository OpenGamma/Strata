/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.function.marketdata.MarketDataTestUtils;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.RateIndexCurveId;

@Test
public class RateIndexCurveMarketDataFunctionTest {

  private static final MarketDataConfig MARKET_DATA_CONFIG = mock(MarketDataConfig.class);

  /**
   * Tests building a single curve
   */
  public void singleCurve() {
    CurveGroup curveGroup = MarketDataTestUtils.curveGroup();
    YieldCurve curve = MarketDataTestUtils.iborIndexCurve(1, IborIndices.EUR_EURIBOR_12M, curveGroup);
    RateIndexCurveId curveId = RateIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, MarketDataTestUtils.CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(MarketDataTestUtils.CURVE_GROUP_NAME);
    BaseMarketData marketData = BaseMarketData.builder(date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    RateIndexCurveMarketDataFunction builder = new RateIndexCurveMarketDataFunction();

    Result<YieldCurve> result = builder.build(curveId, marketData, MARKET_DATA_CONFIG);
    assertThat(result).hasValue(curve);
  }

  /**
   * Tests building multiple curves from the same curve group
   */
  public void multipleCurves() {
    CurveGroup curveGroup = MarketDataTestUtils.curveGroup();
    YieldCurve curve1 = MarketDataTestUtils.iborIndexCurve(1, IborIndices.EUR_EURIBOR_12M, curveGroup);
    YieldCurve curve2 = MarketDataTestUtils.overnightIndexCurve(2, OvernightIndices.CHF_TOIS, curveGroup);
    RateIndexCurveId curveId1 = RateIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, MarketDataTestUtils.CURVE_GROUP_NAME);
    RateIndexCurveId curveId2 = RateIndexCurveId.of(OvernightIndices.CHF_TOIS, MarketDataTestUtils.CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(MarketDataTestUtils.CURVE_GROUP_NAME);
    BaseMarketData marketData = BaseMarketData.builder(date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    RateIndexCurveMarketDataFunction builder = new RateIndexCurveMarketDataFunction();

    Result<YieldCurve> result1 = builder.build(curveId1, marketData, MARKET_DATA_CONFIG);
    assertThat(result1).hasValue(curve1);

    Result<YieldCurve> result2 = builder.build(curveId2, marketData, MARKET_DATA_CONFIG);
    assertThat(result2).hasValue(curve2);
  }

  /**
   * Tests building curves from multiple curve groups
   */
  public void multipleBundles() {
    String groupName1 = "group 1";
    CurveGroup curveGroup1 = MarketDataTestUtils.curveGroup();
    YieldCurve curve1 = MarketDataTestUtils.iborIndexCurve(1, IborIndices.EUR_EURIBOR_12M, curveGroup1);
    YieldCurve curve2 = MarketDataTestUtils.iborIndexCurve(2, IborIndices.CHF_LIBOR_1M, curveGroup1);
    RateIndexCurveId curveId1 = RateIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, groupName1);
    RateIndexCurveId curveId2 = RateIndexCurveId.of(IborIndices.CHF_LIBOR_1M, groupName1);
    CurveGroupId groupId1 = CurveGroupId.of(groupName1);

    String groupName2 = "group 2";
    CurveGroup curveGroup2 = MarketDataTestUtils.curveGroup();
    YieldCurve curve3 = MarketDataTestUtils.overnightIndexCurve(3, OvernightIndices.EUR_EONIA, curveGroup2);
    YieldCurve curve4 = MarketDataTestUtils.overnightIndexCurve(4, OvernightIndices.GBP_SONIA, curveGroup2);
    RateIndexCurveId curveId3 = RateIndexCurveId.of(OvernightIndices.EUR_EONIA, groupName2);
    RateIndexCurveId curveId4 = RateIndexCurveId.of(OvernightIndices.GBP_SONIA, groupName2);
    CurveGroupId groupId2 = CurveGroupId.of(groupName2);

    BaseMarketData marketData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(groupId1, curveGroup1)
            .addValue(groupId2, curveGroup2)
            .build();

    RateIndexCurveMarketDataFunction builder = new RateIndexCurveMarketDataFunction();

    Result<YieldCurve> result1 = builder.build(curveId1, marketData, MARKET_DATA_CONFIG);
    assertThat(result1).hasValue(curve1);

    Result<YieldCurve> result2 = builder.build(curveId2, marketData, MARKET_DATA_CONFIG);
    assertThat(result2).hasValue(curve2);

    Result<YieldCurve> result3 = builder.build(curveId3, marketData, MARKET_DATA_CONFIG);
    assertThat(result3).hasValue(curve3);

    Result<YieldCurve> result4 = builder.build(curveId4, marketData, MARKET_DATA_CONFIG);
    assertThat(result4).hasValue(curve4);
  }
}
