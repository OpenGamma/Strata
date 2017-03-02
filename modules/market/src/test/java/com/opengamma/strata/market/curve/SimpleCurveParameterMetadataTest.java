/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.ValueType;

/**
 * Test {@link SimpleCurveParameterMetadata}.
 */
@Test
public class SimpleCurveParameterMetadataTest {

  public void test_of() {
    SimpleCurveParameterMetadata test = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, 1d);
    assertEquals(test.getXValueType(), ValueType.YEAR_FRACTION);
    assertEquals(test.getXValue(), 1d);
    assertEquals(test.getLabel(), "YearFraction=1.0");
    assertEquals(test.getIdentifier(), "YearFraction=1.0");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleCurveParameterMetadata test = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, 1d);
    coverImmutableBean(test);
    SimpleCurveParameterMetadata test2 = SimpleCurveParameterMetadata.of(ValueType.ZERO_RATE, 2d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SimpleCurveParameterMetadata test = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, 1d);
    assertSerialization(test);
  }

}
