/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.SimpleCurveId;

/**
 * Test {@link SimpleCurveMarketDataFunction}.
 */
@Test
public class SimpleCurveMarketDataFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final CurveGroupName GROUP_NAME = CurveGroupName.of("Group");
  private static final CurveName CURVE_NAME1 = CurveName.of("Name1");
  private static final CurveName CURVE_NAME2 = CurveName.of("Name2");

  //-------------------------------------------------------------------------
  public void test_singleCurve() {
    Curve curve = ConstantNodalCurve.of(CURVE_NAME1, (double) 1);
    SimpleCurveId curveId1 = SimpleCurveId.of(GROUP_NAME, CURVE_NAME1);
    SimpleCurveId curveId2 = SimpleCurveId.of(GROUP_NAME, CURVE_NAME2);
    CurveGroupId groupId = CurveGroupId.of(GROUP_NAME);
    CurveGroup curveGroup = CurveGroup.of(
        GROUP_NAME,
        ImmutableMap.of(Currency.AUD, curve),
        ImmutableMap.of());
    MarketEnvironment marketData = MarketEnvironment.builder(VAL_DATE).addValue(groupId, curveGroup).build();

    SimpleCurveMarketDataFunction test = new SimpleCurveMarketDataFunction();
    MarketDataRequirements reqs = test.requirements(curveId1, MarketDataConfig.empty());
    assertEquals(reqs.getNonObservables(), ImmutableSet.of(groupId));
    MarketDataBox<Curve> result = test.build(curveId1, MarketDataConfig.empty(), marketData, REF_DATA);
    assertEquals(result, MarketDataBox.ofSingleValue(curve));
    assertThrowsIllegalArg(() -> test.build(curveId2, MarketDataConfig.empty(), marketData, REF_DATA));
  }

  public void test_multipleCurves() {
    Curve curve1 = ConstantNodalCurve.of(CURVE_NAME1, (double) 1);
    Curve curve2 = ConstantNodalCurve.of(CURVE_NAME2, (double) 2);
    SimpleCurveId curveId1 = SimpleCurveId.of(GROUP_NAME, CURVE_NAME1);
    SimpleCurveId curveId2 = SimpleCurveId.of(GROUP_NAME, CURVE_NAME2);
    CurveGroupId groupId = CurveGroupId.of(GROUP_NAME);
    CurveGroup curveGroup = CurveGroup.of(
        GROUP_NAME,
        ImmutableMap.of(Currency.AUD, curve1, Currency.GBP, curve2),
        ImmutableMap.of());
    MarketEnvironment marketData = MarketEnvironment.builder(VAL_DATE).addValue(groupId, curveGroup).build();

    SimpleCurveMarketDataFunction test = new SimpleCurveMarketDataFunction();
    MarketDataBox<Curve> result1 = test.build(curveId1, MarketDataConfig.empty(), marketData, REF_DATA);
    assertEquals(result1, MarketDataBox.ofSingleValue(curve1));
    MarketDataBox<Curve> result2 = test.build(curveId2, MarketDataConfig.empty(), marketData, REF_DATA);
    assertEquals(result2, MarketDataBox.ofSingleValue(curve2));
  }

}
