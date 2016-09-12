/**
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
 * Test {@link CurveId}.
 */
@Test
public class CurveIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_String() {
    CurveId test = CurveId.of("Group", "Name");
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  public void test_of_Types() {
    CurveId test = CurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"));
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  public void test_of_TypesSource() {
    CurveId test = CurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), OBS_SOURCE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveId test = CurveId.of("Group", "Name");
    coverImmutableBean(test);
    CurveId test2 = CurveId.of("Group2", "Name2");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveId test = CurveId.of("Group", "Name");
    assertSerialization(test);
  }

}
