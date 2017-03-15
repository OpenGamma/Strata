/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.FxMatrix;

/**
 * Test {@link FxMatrixId}.
 */
@Test
public class FxMatrixIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Test");

  //-------------------------------------------------------------------------
  public void test_standard() {
    FxMatrixId test = FxMatrixId.standard();
    assertEquals(test.getObservableSource(), ObservableSource.NONE);
    assertEquals(test.getMarketDataType(), FxMatrix.class);
  }

  public void test_of() {
    FxMatrixId test = FxMatrixId.of(OBS_SOURCE);
    assertEquals(test.getObservableSource(), OBS_SOURCE);
    assertEquals(test.getMarketDataType(), FxMatrix.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxMatrixId test = FxMatrixId.standard();
    coverImmutableBean(test);
    FxMatrixId test2 = FxMatrixId.of(OBS_SOURCE);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxMatrixId test = FxMatrixId.standard();
    assertSerialization(test);
  }

}
