/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.IborIndexCurveId;

/**
 * Test {@link IborIndexCurveMarketDataFunction}.
 */
@Test
public class IborIndexCurveMarketDataFunctionTest {

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("group name");

  /**
   * Tests building a single curve
   */
  public void singleCurve() {
    Curve curve = ConstantNodalCurve.of(IborIndices.EUR_EURIBOR_12M.getName(), 1);
    IborIndexCurveId curveId = IborIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, CURVE_GROUP_NAME);
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
    IborIndexCurveMarketDataFunction builder = new IborIndexCurveMarketDataFunction();

    MarketDataBox<Curve> result = builder.build(curveId, marketData, MarketDataConfig.empty());
    assertThat(result).isEqualTo(MarketDataBox.ofSingleValue(curve));
  }

  /**
   * Tests building multiple curves from the same curve group
   */
  public void multipleCurves() {
    Curve curve1 = ConstantNodalCurve.of(IborIndices.EUR_EURIBOR_12M.getName(), 1);
    Curve curve2 = ConstantNodalCurve.of(IborIndices.CHF_LIBOR_3M.getName(), 1);
    IborIndexCurveId curveId1 = IborIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, CURVE_GROUP_NAME);
    IborIndexCurveId curveId2 = IborIndexCurveId.of(IborIndices.CHF_LIBOR_3M, CURVE_GROUP_NAME);
    CurveGroupId groupId = CurveGroupId.of(CURVE_GROUP_NAME);
    Map<Index, Curve> curveMap = ImmutableMap.of(
        IborIndices.EUR_EURIBOR_12M, curve1,
        IborIndices.CHF_LIBOR_3M, curve2);
    CurveGroup curveGroup = CurveGroup.builder()
        .name(CurveGroupName.of("groupName"))
        .forwardCurves(curveMap)
        .build();
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(groupId, curveGroup)
        .build();
    IborIndexCurveMarketDataFunction builder = new IborIndexCurveMarketDataFunction();

    MarketDataBox<Curve> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).isEqualTo(MarketDataBox.ofSingleValue(curve1));

    MarketDataBox<Curve> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).isEqualTo(MarketDataBox.ofSingleValue(curve2));
  }

  /**
   * Tests building curves from multiple curve groups
   */
  public void multipleBundles() {
    CurveGroupName groupName1 = CurveGroupName.of("group 1");
    Curve curve1 = ConstantNodalCurve.of(IborIndices.EUR_EURIBOR_12M.getName(), (double) 1);
    Curve curve2 = ConstantNodalCurve.of(IborIndices.CHF_LIBOR_1M.getName(), (double) 2);
    IborIndexCurveId curveId1 = IborIndexCurveId.of(IborIndices.EUR_EURIBOR_12M, groupName1);
    IborIndexCurveId curveId2 = IborIndexCurveId.of(IborIndices.CHF_LIBOR_1M, groupName1);
    CurveGroupId groupId1 = CurveGroupId.of(groupName1);
    Map<Index, Curve> curveMap1 = ImmutableMap.of(
        IborIndices.EUR_EURIBOR_12M, curve1,
        IborIndices.CHF_LIBOR_1M, curve2);
    CurveGroup curveGroup1 = CurveGroup.builder()
        .name(groupName1)
        .forwardCurves(curveMap1)
        .build();

    CurveGroupName groupName2 = CurveGroupName.of("group 2");
    Curve curve3 = ConstantNodalCurve.of(IborIndices.USD_LIBOR_12M.getName(), (double) 3);
    Curve curve4 = ConstantNodalCurve.of(IborIndices.GBP_LIBOR_12M.getName(), (double) 4);
    IborIndexCurveId curveId3 = IborIndexCurveId.of(IborIndices.USD_LIBOR_12M, groupName2);
    IborIndexCurveId curveId4 = IborIndexCurveId.of(IborIndices.GBP_LIBOR_12M, groupName2);
    CurveGroupId groupId2 = CurveGroupId.of(groupName2);
    Map<Index, Curve> curveMap2 = ImmutableMap.of(
        IborIndices.USD_LIBOR_12M, curve3,
        IborIndices.GBP_LIBOR_12M, curve4);
    CurveGroup curveGroup2 = CurveGroup.builder()
        .name(groupName2)
        .forwardCurves(curveMap2)
        .build();

    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(groupId1, curveGroup1)
        .addValue(groupId2, curveGroup2)
        .build();

    IborIndexCurveMarketDataFunction builder = new IborIndexCurveMarketDataFunction();

    MarketDataBox<Curve> result1 = builder.build(curveId1, marketData, MarketDataConfig.empty());
    assertThat(result1).isEqualTo(MarketDataBox.ofSingleValue(curve1));

    MarketDataBox<Curve> result2 = builder.build(curveId2, marketData, MarketDataConfig.empty());
    assertThat(result2).isEqualTo(MarketDataBox.ofSingleValue(curve2));

    MarketDataBox<Curve> result3 = builder.build(curveId3, marketData, MarketDataConfig.empty());
    assertThat(result3).isEqualTo(MarketDataBox.ofSingleValue(curve3));

    MarketDataBox<Curve> result4 = builder.build(curveId4, marketData, MarketDataConfig.empty());
    assertThat(result4).isEqualTo(MarketDataBox.ofSingleValue(curve4));
  }

}
