/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.ValueType;

/**
 * Test {@link SimpleCubeParameterMetadata}.
 */
public class SimpleCubeParameterMetadataTest {

  @Test
  public void test_of() {
    SimpleCubeParameterMetadata test = SimpleCubeParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d, ValueType.NORMAL_VOLATILITY, 2d);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getXValue()).isEqualTo(1d);
    assertThat(test.getYValueType()).isEqualTo(ValueType.STRIKE);
    assertThat(test.getYValue()).isEqualTo(3d);
    assertThat(test.getZValueType()).isEqualTo(ValueType.NORMAL_VOLATILITY);
    assertThat(test.getZValue()).isEqualTo(2d);
    assertThat(test.getLabel()).isEqualTo("YearFraction=1.0, Strike=3.0, NormalVolatility=2.0");
    assertThat(test.getIdentifier()).isEqualTo("YearFraction=1.0, Strike=3.0, NormalVolatility=2.0");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleCubeParameterMetadata test = SimpleCubeParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d, ValueType.NORMAL_VOLATILITY, 2d);
    coverImmutableBean(test);
    SimpleCubeParameterMetadata test2 = SimpleCubeParameterMetadata.of(
        ValueType.ZERO_RATE, 2d, ValueType.SIMPLE_MONEYNESS, 4d, ValueType.LOCAL_VOLATILITY, 7d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SimpleCubeParameterMetadata test = SimpleCubeParameterMetadata.of(
        ValueType.YEAR_FRACTION, 1d, ValueType.STRIKE, 3d, ValueType.NORMAL_VOLATILITY, 2d);
    assertSerialization(test);
  }

}
