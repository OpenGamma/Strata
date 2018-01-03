/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link RepoCurveId}.
 */
@Test
public class RepoCurveIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_String() {
    RepoCurveId test = RepoCurveId.of("Group", "Name");
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  public void test_of_Types() {
    RepoCurveId test = RepoCurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"));
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  public void test_of_TypesSource() {
    RepoCurveId test = RepoCurveId.of(
        CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), OBS_SOURCE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RepoCurveId test1 = RepoCurveId.of(
        CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    coverImmutableBean(test1);
    RepoCurveId test2 = RepoCurveId.of("Group2", "Name2");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    RepoCurveId test = RepoCurveId.of("Group", "Name");
    assertSerialization(test);
  }

}
