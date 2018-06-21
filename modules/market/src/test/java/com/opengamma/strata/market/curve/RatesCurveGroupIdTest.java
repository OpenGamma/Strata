/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link RatesCurveGroupId}.
 */
@Test
public class RatesCurveGroupIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final ObservableSource OBS_SOURCE2 = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void test_of_String() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1.toString());
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), RatesCurveGroup.class);
    assertEquals(test.toString(), "RatesCurveGroupId:Group1");
  }

  public void test_of_Type() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), RatesCurveGroup.class);
    assertEquals(test.toString(), "RatesCurveGroupId:Group1");
  }

  public void test_of_TypeSource() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1, OBS_SOURCE2);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getObservableSource(), OBS_SOURCE2);
    assertEquals(test.getMarketDataType(), RatesCurveGroup.class);
    assertEquals(test.toString(), "RatesCurveGroupId:Group1/Vendor");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1);
    coverImmutableBean(test);
    RatesCurveGroupId test2 = RatesCurveGroupId.of(GROUP2, OBS_SOURCE2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1);
    assertSerialization(test);
  }

}
