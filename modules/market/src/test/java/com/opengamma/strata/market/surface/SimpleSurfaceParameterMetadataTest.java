/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.ValueType;

/**
 * Test {@link SimpleSurfaceParameterMetadata}.
 */
public class SimpleSurfaceParameterMetadataTest {

  @Test
  public void test_of() {
    SimpleSurfaceParameterMetadata test = SimpleSurfaceParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getXValue()).isEqualTo(1d);
    assertThat(test.getYValueType()).isEqualTo(ValueType.STRIKE);
    assertThat(test.getYValue()).isEqualTo(3d);
    assertThat(test.getLabel()).isEqualTo("YearFraction=1.0, Strike=3.0");
    assertThat(test.getIdentifier()).isEqualTo("YearFraction=1.0, Strike=3.0");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleSurfaceParameterMetadata test = SimpleSurfaceParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d);
    coverImmutableBean(test);
    SimpleSurfaceParameterMetadata test2 = SimpleSurfaceParameterMetadata.of(
        ValueType.ZERO_RATE, 2d, ValueType.SIMPLE_MONEYNESS, 4d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SimpleSurfaceParameterMetadata test = SimpleSurfaceParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d);
    assertSerialization(test);
  }

}
