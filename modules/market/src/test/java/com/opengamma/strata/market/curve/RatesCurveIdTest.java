/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link RatesCurveId}.
 */
@Test
public class RatesCurveIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_String() {
    RatesCurveId test = RatesCurveId.of("Group", "Name");
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
    assertEquals(test.toString(), "RatesCurveId:Group/Name");
  }

  public void test_of_Types() {
    RatesCurveId test = RatesCurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"));
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
    assertEquals(test.toString(), "RatesCurveId:Group/Name");
  }

  public void test_of_TypesSource() {
    RatesCurveId test = RatesCurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), OBS_SOURCE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
    assertEquals(test.toString(), "RatesCurveId:Group/Name/Vendor");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RatesCurveId test = RatesCurveId.of("Group", "Name");
    coverImmutableBean(test);
    RatesCurveId test2 = RatesCurveId.of("Group2", "Name2");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RatesCurveId test = RatesCurveId.of("Group", "Name");
    assertSerialization(test);
  }

}
