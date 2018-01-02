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
 * Test {@link IssuerCurveId}.
 */
@Test
public class IssuerCurveIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_String() {
    IssuerCurveId test = IssuerCurveId.of("Group", "Name");
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  public void test_of_Types() {
    IssuerCurveId test = IssuerCurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"));
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  public void test_of_TypesSource() {
    IssuerCurveId test = IssuerCurveId.of(
        CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), OBS_SOURCE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IssuerCurveId test1 = IssuerCurveId.of(
        CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    coverImmutableBean(test1);
    IssuerCurveId test2 = IssuerCurveId.of("Group2", "Name2");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IssuerCurveId test = IssuerCurveId.of("Group", "Name");
    assertSerialization(test);
  }

}
