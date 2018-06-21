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
 * Test {@link IssuerCurveInputsId}.
 */
@Test
public class IssuerCurveInputsIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final CurveName NAME1 = CurveName.of("Name1");
  private static final CurveName NAME2 = CurveName.of("Name2");
  private static final ObservableSource SOURCE2 = ObservableSource.of("Vendor2");

  //-------------------------------------------------------------------------
  public void test_of() {
    IssuerCurveInputsId test = IssuerCurveInputsId.of(GROUP1, NAME1, ObservableSource.NONE);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getCurveName(), NAME1);
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), RatesCurveInputs.class);
    assertEquals(test.toString(), "IssuerCurveInputsId:Group1/Name1");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IssuerCurveInputsId test = IssuerCurveInputsId.of(GROUP1, NAME1, ObservableSource.NONE);
    coverImmutableBean(test);
    IssuerCurveInputsId test2 = IssuerCurveInputsId.of(GROUP2, NAME2, SOURCE2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IssuerCurveInputsId test = IssuerCurveInputsId.of(GROUP1, NAME1, ObservableSource.NONE);
    assertSerialization(test);
  }

}
