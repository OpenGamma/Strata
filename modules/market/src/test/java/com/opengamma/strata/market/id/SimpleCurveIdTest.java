/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link SimpleCurveId}.
 */
@Test
public class SimpleCurveIdTest {

  //-------------------------------------------------------------------------
  public void test_of_String() {
    SimpleCurveId test = SimpleCurveId.of("Group", "Name");
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.toMarketDataId(null), test);
    assertEquals(test.toMarketDataKey(), test);
  }

  public void test_of_TYpes() {
    SimpleCurveId test = SimpleCurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"));
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.toMarketDataId(null), test);
    assertEquals(test.toMarketDataKey(), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleCurveId test = SimpleCurveId.of("Group", "Name");
    coverImmutableBean(test);
    SimpleCurveId test2 = SimpleCurveId.of("Group2", "Name2");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SimpleCurveId test = SimpleCurveId.of("Group", "Name");
    assertSerialization(test);
  }

}
