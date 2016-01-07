/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;

/**
 * Test {@link CurveGroupKey}.
 */
@Test
public class CurveGroupKeyTest {

  //-------------------------------------------------------------------------
  public void test_of_String() {
    CurveGroupKey test = CurveGroupKey.of("foo");
    assertEquals(test.getName(), CurveGroupName.of("foo"));
    assertEquals(test.getMarketDataType(), CurveGroup.class);
  }

  public void test_of_CurveGroupName() {
    CurveGroupKey test = CurveGroupKey.of(CurveGroupName.of("foo"));
    assertEquals(test.getName(), CurveGroupName.of("foo"));
    assertEquals(test.getMarketDataType(), CurveGroup.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupKey test = CurveGroupKey.of("foo");
    coverImmutableBean(test);
    CurveGroupKey test2 = CurveGroupKey.of("bar");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupKey test = CurveGroupKey.of("foo");
    assertSerialization(test);
  }

}
