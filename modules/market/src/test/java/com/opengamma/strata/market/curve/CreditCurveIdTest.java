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
 * Test {@link CreditCurveId}.
 */
@Test
public class CreditCurveIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_String() {
    CreditCurveId test = CreditCurveId.of("Group", "Name");
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
    assertEquals(test.toString(), "CreditCurveId:Group/Name");
  }

  public void test_of_Types() {
    CreditCurveId test = CreditCurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"));
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
    assertEquals(test.toString(), "CreditCurveId:Group/Name");
  }

  public void test_of_TypesSource() {
    CreditCurveId test = CreditCurveId.of(
        CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("Group"));
    assertEquals(test.getCurveName(), CurveName.of("Name"));
    assertEquals(test.getObservableSource(), OBS_SOURCE);
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.getMarketDataName(), CurveName.of("Name"));
    assertEquals(test.toString(), "CreditCurveId:Group/Name/Vendor");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CreditCurveId test1 = CreditCurveId.of(
        CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    coverImmutableBean(test1);
    CreditCurveId test2 = CreditCurveId.of("Group2", "Name2");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CreditCurveId test = CreditCurveId.of("Group", "Name");
    assertSerialization(test);
  }

}
