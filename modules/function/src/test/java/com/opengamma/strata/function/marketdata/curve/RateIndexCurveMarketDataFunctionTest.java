/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.RateIndexCurveId;

/**
 * Test {@link RateIndexCurveMarketDataFunction}.
 */
@Test
public class RateIndexCurveMarketDataFunctionTest {

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("group name");

  /**
   * Tests building a single curve
   */
  public void singleCurve() {
    Curve curve = ConstantNodalCurve.of(IborIndices.EUR_EURIBOR_12M.getName(), 1);
    RateIndexCurveId curveId = RateIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(CURVE_GROUP_NAME);
    Map<Index, Curve> curveMap = ImmutableMap.of(IborIndices.EUR_EURIBOR_12M, curve);
    CurveGroup curveGroup = CurveGroup.builder()
        .name(CurveGroupName.of("groupName"))
        .forwardCurves(curveMap)
        .build();
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(groupId, curveGroup)
        .build();
    RateIndexCurveMarketDataFunction builder = new RateIndexCurveMarketDataFunction();

    Result<MarketDataBox<Curve>> result = builder.build(curveId, marketData, MarketDataConfig.empty());
    assertThat(result).hasValue(MarketDataBox.ofSingleValue(curve));
  }

  /**
   * Tests building multiple curves from the same curve group
   */
  public void multipleCurves() {
    Curve curve1 = ConstantNodalCurve.of(IborIndices.EUR_EURIBOR_12M.getName(), 1);
    Curve curve2 = ConstantNodalCurve.of(OvernightIndices.CHF_TOIS.getName(), 1);
    RateIndexCurveId curveId1 = RateIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, CURVE_GROUP_NAME);
    RateIndexCurveId curveId2 = RateIndexCurveId.of(OvernightIndices.CHF_TOIS, CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(CURVE_GROUP_NAME);
    Map<Index, Curve> curveMap = ImmutableMap.of(
        IborIndices.EUR_EURIBOR_12M, curve1,
        OvernightIndices.CHF_TOIS, curve2);
    CurveGroup curveGroup = CurveGroup.builder()
        .name(CurveGroupName.of("groupName"))
        .forwardCurves(curveMap)
        .build();
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(groupId, curveGroup)
        .build();
    RateIndexCurveMarketDataFunction builder = new RateIndexCurveMarketDataFunction();

    Result<MarketDataBox<Curve>> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).hasValue(MarketDataBox.ofSingleValue(curve1));

    Result<MarketDataBox<Curve>> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).hasValue(MarketDataBox.ofSingleValue(curve2));
  }

  /**
   * Tests building curves from multiple curve groups
   */
  public void multipleBundles() {
    CurveGroupName groupName1 = CurveGroupName.of("group 1");
    Curve curve1 = ConstantNodalCurve.of(IborIndices.EUR_EURIBOR_12M.getName(), (double) 1);
    Curve curve2 = ConstantNodalCurve.of(IborIndices.CHF_LIBOR_1M.getName(), (double) 2);
    RateIndexCurveId curveId1 = RateIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, groupName1);
    RateIndexCurveId curveId2 = RateIndexCurveId.of(IborIndices.CHF_LIBOR_1M, groupName1);
    CurveGroupId groupId1 = CurveGroupId.of(groupName1);
    Map<Index, Curve> curveMap1 = ImmutableMap.of(
        IborIndices.EUR_EURIBOR_12M, curve1,
        IborIndices.CHF_LIBOR_1M, curve2);
    CurveGroup curveGroup1 = CurveGroup.builder()
        .name(groupName1)
        .forwardCurves(curveMap1)
        .build();

    CurveGroupName groupName2 = CurveGroupName.of("group 2");
    Curve curve3 = ConstantNodalCurve.of(OvernightIndices.EUR_EONIA.getName(), (double) 3);
    Curve curve4 = ConstantNodalCurve.of(OvernightIndices.GBP_SONIA.getName(), (double) 4);
    RateIndexCurveId curveId3 = RateIndexCurveId.of(OvernightIndices.EUR_EONIA, groupName2);
    RateIndexCurveId curveId4 = RateIndexCurveId.of(OvernightIndices.GBP_SONIA, groupName2);
    CurveGroupId groupId2 = CurveGroupId.of(groupName2);
    Map<Index, Curve> curveMap2 = ImmutableMap.of(
        OvernightIndices.EUR_EONIA, curve3,
        OvernightIndices.GBP_SONIA, curve4);
    CurveGroup curveGroup2 = CurveGroup.builder()
        .name(groupName2)
        .forwardCurves(curveMap2)
        .build();

    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(groupId1, curveGroup1)
        .addValue(groupId2, curveGroup2)
        .build();

    RateIndexCurveMarketDataFunction builder = new RateIndexCurveMarketDataFunction();

    Result<MarketDataBox<Curve>> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).hasValue(MarketDataBox.ofSingleValue(curve1));

    Result<MarketDataBox<Curve>> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).hasValue(MarketDataBox.ofSingleValue(curve2));

    Result<MarketDataBox<Curve>> result3 = builder.build(curveId3, marketData, MarketDataConfig.empty());
    assertThat(result3).hasValue(MarketDataBox.ofSingleValue(curve3));

    Result<MarketDataBox<Curve>> result4 = builder.build(curveId4, marketData, MarketDataConfig.empty());
    assertThat(result4).hasValue(MarketDataBox.ofSingleValue(curve4));
  }

}
