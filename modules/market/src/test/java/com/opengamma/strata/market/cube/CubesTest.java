/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.ValueType;

/**
 * Test {@link Cubes}.
 */
public class CubesTest {

  private static final String NAME = "Foo";
  private static final CubeName CUBE_NAME = CubeName.of(NAME);

  @Test
  public void test_normalVolatilityByExpiryTenorStrike_string() {
    CubeMetadata test = Cubes.normalVolatilityByExpiryTenorStrike(NAME, ACT_360);
    CubeMetadata expected = DefaultCubeMetadata.builder()
        .cubeName(CUBE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.STRIKE)
        .wValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_normalVolatilityByExpiryTenorStrike_cubeName() {
    CubeMetadata test = Cubes.normalVolatilityByExpiryTenorStrike(CUBE_NAME, ACT_360);
    CubeMetadata expected = DefaultCubeMetadata.builder()
        .cubeName(CUBE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.STRIKE)
        .wValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

}
