/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.ValueType;

/**
 * Test {@link SimpleCurveParameterMetadata}.
 */
public class SimpleCurveParameterMetadataTest {

  @Test
  public void test_of() {
    SimpleCurveParameterMetadata test = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, 1d);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getXValue()).isEqualTo(1d);
    assertThat(test.getLabel()).isEqualTo("YearFraction=1.0");
    assertThat(test.getIdentifier()).isEqualTo("YearFraction=1.0");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleCurveParameterMetadata test = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, 1d);
    coverImmutableBean(test);
    SimpleCurveParameterMetadata test2 = SimpleCurveParameterMetadata.of(ValueType.ZERO_RATE, 2d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SimpleCurveParameterMetadata test = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, 1d);
    assertSerialization(test);
  }

}
