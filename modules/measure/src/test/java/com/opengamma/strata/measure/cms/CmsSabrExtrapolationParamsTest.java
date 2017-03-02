/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link CmsSabrExtrapolationParams}.
 */
@Test
public class CmsSabrExtrapolationParamsTest {

  public void test_of() {
    CmsSabrExtrapolationParams test = CmsSabrExtrapolationParams.of(1d, 2d);
    assertEquals(test.getCutOffStrike(), 1d);
    assertEquals(test.getMu(), 2d);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CmsSabrExtrapolationParams test = CmsSabrExtrapolationParams.of(1d, 2d);
    coverImmutableBean(test);
    CmsSabrExtrapolationParams test2 = CmsSabrExtrapolationParams.of(3d, 4d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CmsSabrExtrapolationParams test = CmsSabrExtrapolationParams.of(1d, 2d);
    assertSerialization(test);
  }

}
