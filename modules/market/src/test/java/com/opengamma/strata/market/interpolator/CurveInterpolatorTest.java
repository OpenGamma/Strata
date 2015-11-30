/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.market.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.interpolator.CurveInterpolators.LOG_LINEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;

/**
 * Test {@link CurveInterpolator}.
 */
@Test
public class CurveInterpolatorTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {LINEAR, "Linear"},
        {LOG_LINEAR, "LogLinear"},
        {DOUBLE_QUADRATIC, "DoubleQuadratic"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(CurveInterpolator convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(CurveInterpolator convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CurveInterpolator convention, String name) {
    assertEquals(CurveInterpolator.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(CurveInterpolator convention, String name) {
    ImmutableMap<String, CurveInterpolator> map = CurveInterpolator.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> CurveInterpolator.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> CurveInterpolator.of(null));
  }

  public void test_LINEAR() {
    assertTrue(LINEAR instanceof ImmutableCurveInterpolator);
    assertTrue(((ImmutableCurveInterpolator) LINEAR).getUnderlying() instanceof LinearInterpolator1D);
  }

  //-------------------------------------------------------------------------
  public void test_bind() {
    DoubleArray xValues = DoubleArray.of(1, 2, 3);
    DoubleArray yValues = DoubleArray.of(2, 4, 5);
    BoundCurveInterpolator bound = LINEAR.bind(xValues, yValues, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
    assertEquals(bound.yValue(0.5), 2d, 0d);
    assertEquals(bound.yValue(1), 2d, 0d);
    assertEquals(bound.yValue(1.5), 3d, 0d);
    assertEquals(bound.yValue(2), 4d, 0d);
    assertEquals(bound.yValue(2.5), 4.5d, 0d);
    assertEquals(bound.yValue(3), 5d, 0d);
    assertEquals(bound.yValue(3.5), 5d, 0d);
    // coverage
    assertEquals(bound.yValueParameterSensitivity(0.5).size(), 3);
    assertEquals(bound.yValueParameterSensitivity(2).size(), 3);
    assertEquals(bound.yValueParameterSensitivity(3.5).size(), 3);
    assertEquals(bound.firstDerivative(0.5), 0d, 0d);
    assertTrue(bound.firstDerivative(2) != 0d);
    assertEquals(bound.firstDerivative(3.5), 0d, 0d);
    assertNotNull(bound.toString());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(CurveInterpolators.class);
    coverPrivateConstructor(StandardCurveInterpolators.class);
    assertFalse(LINEAR.equals(null));
    assertFalse(LINEAR.equals(""));
  }

  public void test_serialization() {
    assertSerialization(LINEAR);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CurveInterpolator.class, LINEAR);
    assertJodaConvert(CurveInterpolator.class, LOG_LINEAR);
  }

}
