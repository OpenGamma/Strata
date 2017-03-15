/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.ValueType;

/**
 * Test {@link SimpleSurfaceParameterMetadata}.
 */
@Test
public class SimpleSurfaceParameterMetadataTest {

  public void test_of() {
    SimpleSurfaceParameterMetadata test = SimpleSurfaceParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d);
    assertEquals(test.getXValueType(), ValueType.YEAR_FRACTION);
    assertEquals(test.getXValue(), 1d);
    assertEquals(test.getYValueType(), ValueType.STRIKE);
    assertEquals(test.getYValue(), 3d);
    assertEquals(test.getLabel(), "YearFraction=1.0, Strike=3.0");
    assertEquals(test.getIdentifier(), "YearFraction=1.0, Strike=3.0");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleSurfaceParameterMetadata test = SimpleSurfaceParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d);
    coverImmutableBean(test);
    SimpleSurfaceParameterMetadata test2 = SimpleSurfaceParameterMetadata.of(
        ValueType.ZERO_RATE, 2d, ValueType.SIMPLE_MONEYNESS, 4d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SimpleSurfaceParameterMetadata test = SimpleSurfaceParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d);
    assertSerialization(test);
  }

}
