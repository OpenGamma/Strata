/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.builders;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataTestUtils;
import com.opengamma.strata.marketdata.curve.CurveGroup;
import com.opengamma.strata.marketdata.id.CurveGroupId;
import com.opengamma.strata.marketdata.id.IndexCurveId;

@Test
public class IndexCurveMarketDataBuilderTest {

  /**
   * Tests building a single curve
   */
  public void singleCurve() {
    CurveGroup curveGroup = MarketDataTestUtils.curveGroup();
    YieldCurve curve = MarketDataTestUtils.iborIndexCurve(1, IborIndices.EUR_EURIBOR_12M, curveGroup);
    IndexCurveId curveId = IndexCurveId.of(IborIndices.EUR_EURIBOR_12M, MarketDataTestUtils.CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(MarketDataTestUtils.CURVE_GROUP_NAME);
    BaseMarketData marketData = BaseMarketData.builder(date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    IndexCurveMarketDataBuilder builder = new IndexCurveMarketDataBuilder();

    Result<YieldCurve> result = builder.buildSingleValue(curveId, marketData);
    assertThat(result).hasValue(curve);
  }

  /**
   * Tests building multiple curves from the same curve group
   */
  public void multipleCurves() {
    CurveGroup curveGroup = MarketDataTestUtils.curveGroup();
    YieldCurve curve1 = MarketDataTestUtils.iborIndexCurve(1, IborIndices.EUR_EURIBOR_12M, curveGroup);
    YieldCurve curve2 = MarketDataTestUtils.overnightIndexCurve(2, OvernightIndices.CHF_TOIS, curveGroup);
    IndexCurveId curveId1 = IndexCurveId.of(IborIndices.EUR_EURIBOR_12M, MarketDataTestUtils.CURVE_GROUP_NAME);
    IndexCurveId curveId2 = IndexCurveId.of(OvernightIndices.CHF_TOIS, MarketDataTestUtils.CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(MarketDataTestUtils.CURVE_GROUP_NAME);
    BaseMarketData marketData = BaseMarketData.builder(date(2011, 3, 8)).addValue(groupId, curveGroup).build();
    IndexCurveMarketDataBuilder builder = new IndexCurveMarketDataBuilder();

    Result<YieldCurve> result1 = builder.buildSingleValue(curveId1, marketData);
    assertThat(result1).hasValue(curve1);

    Result<YieldCurve> result2 = builder.buildSingleValue(curveId2, marketData);
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
    IndexCurveId curveId1 = IndexCurveId.of(IborIndices.EUR_EURIBOR_12M, groupName1);
    IndexCurveId curveId2 = IndexCurveId.of(IborIndices.CHF_LIBOR_1M, groupName1);
    CurveGroupId groupId1 = CurveGroupId.of(groupName1);

    String groupName2 = "group 2";
    CurveGroup curveGroup2 = MarketDataTestUtils.curveGroup();
    YieldCurve curve3 = MarketDataTestUtils.overnightIndexCurve(3, OvernightIndices.EUR_EONIA, curveGroup2);
    YieldCurve curve4 = MarketDataTestUtils.overnightIndexCurve(4, OvernightIndices.GBP_SONIA, curveGroup2);
    IndexCurveId curveId3 = IndexCurveId.of(OvernightIndices.EUR_EONIA, groupName2);
    IndexCurveId curveId4 = IndexCurveId.of(OvernightIndices.GBP_SONIA, groupName2);
    CurveGroupId groupId2 = CurveGroupId.of(groupName2);

    BaseMarketData marketData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(groupId1, curveGroup1)
            .addValue(groupId2, curveGroup2)
            .build();

    IndexCurveMarketDataBuilder builder = new IndexCurveMarketDataBuilder();

    Result<YieldCurve> result1 = builder.buildSingleValue(curveId1, marketData);
    assertThat(result1).hasValue(curve1);

    Result<YieldCurve> result2 = builder.buildSingleValue(curveId2, marketData);
    assertThat(result2).hasValue(curve2);

    Result<YieldCurve> result3 = builder.buildSingleValue(curveId3, marketData);
    assertThat(result3).hasValue(curve3);

    Result<YieldCurve> result4 = builder.buildSingleValue(curveId4, marketData);
    assertThat(result4).hasValue(curve4);
  }
}
