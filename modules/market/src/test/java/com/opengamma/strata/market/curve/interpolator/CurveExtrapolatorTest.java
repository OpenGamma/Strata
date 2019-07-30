/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LOG_LINEAR;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link CurveExtrapolator}.
 */
@Test
public class CurveExtrapolatorTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {CurveExtrapolators.EXCEPTION, "Exception"},
        {CurveExtrapolators.EXPONENTIAL, "Exponential"},
        {CurveExtrapolators.FLAT, "Flat"},
        {CurveExtrapolators.INTERPOLATOR, "Interpolator"},
        {CurveExtrapolators.LINEAR, "Linear"},
        {CurveExtrapolators.LOG_LINEAR, "LogLinear"},
        {CurveExtrapolators.PRODUCT_LINEAR, "ProductLinear"},
        {CurveExtrapolators.QUADRATIC_LEFT, "QuadraticLeft"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(CurveExtrapolator convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(CurveExtrapolator convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CurveExtrapolator convention, String name) {
    assertEquals(CurveExtrapolator.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(CurveExtrapolator convention, String name) {
    ImmutableMap<String, CurveExtrapolator> map = CurveExtrapolator.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveExtrapolator.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveExtrapolator.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(CurveExtrapolators.class);
    coverPrivateConstructor(StandardCurveExtrapolators.class);
    assertFalse(FLAT.equals(null));
    assertFalse(FLAT.equals(ANOTHER_TYPE));
  }

  public void test_serialization() {
    assertSerialization(FLAT);
    assertSerialization(LINEAR);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CurveExtrapolator.class, FLAT);
    assertJodaConvert(CurveExtrapolator.class, LOG_LINEAR);
  }

}
