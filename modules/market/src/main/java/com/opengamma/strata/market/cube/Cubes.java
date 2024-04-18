/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.market.ValueType;

/**
 * Helper for creating common types of cubes.
 */
public final class Cubes {

  /**
   * Restricted constructor.
   */
  private Cubes() {
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a cube providing normal expiry-tenor-strike volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * The z-values represent strike.
   * The z-values represent normal volatility.
   *
   * @param name  the cube name
   * @param dayCount  the day count
   * @return the cube metadata
   */
  public static CubeMetadata normalVolatilityByExpiryTenorStrike(String name, DayCount dayCount) {
    return normalVolatilityByExpiryTenorStrike(CubeName.of(name), dayCount);
  }

  /**
   * Creates metadata for a cube providing normal expiry-tenor-strike volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * The z-values represent strike.
   * The w-values represent normal volatility.
   *
   * @param name  the cube name
   * @param dayCount  the day count
   * @return the cube metadata
   */
  public static CubeMetadata normalVolatilityByExpiryTenorStrike(CubeName name, DayCount dayCount) {
    return DefaultCubeMetadata.builder()
        .cubeName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.STRIKE)
        .wValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

}
