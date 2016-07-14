/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * Helper for creating common types of surfaces.
 */
public final class Surfaces {

  /**
   * Restricted constructor.
   */
  private Surfaces() {
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Black expiry-tenor volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata blackVolatilityByExpiryTenor(String name, DayCount dayCount) {
    return blackVolatilityByExpiryTenor(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Black expiry-tenor volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata blackVolatilityByExpiryTenor(SurfaceName name, DayCount dayCount) {
    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Black expiry-strike volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata blackVolatilityByExpiryStrike(String name, DayCount dayCount) {
    return blackVolatilityByExpiryStrike(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Black expiry-strike volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata blackVolatilityByExpiryStrike(SurfaceName name, DayCount dayCount) {
    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Black expiry-log moneyness volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent log-moneyness
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata blackVolatilityByExpiryLogMoneyness(String name, DayCount dayCount) {
    return blackVolatilityByExpiryLogMoneyness(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Black expiry-log moneyness volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent log-moneyness
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata blackVolatilityByExpiryLogMoneyness(SurfaceName name, DayCount dayCount) {
    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.LOG_MONEYNESS)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Normal expiry-tenor volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata normalVolatilityByExpiryTenor(String name, DayCount dayCount) {
    return normalVolatilityByExpiryTenor(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-tenor volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata normalVolatilityByExpiryTenor(SurfaceName name, DayCount dayCount) {
    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Normal expiry-strike volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata normalVolatilityByExpiryStrike(String name, DayCount dayCount) {
    return normalVolatilityByExpiryStrike(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-strike volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata normalVolatilityByExpiryStrike(SurfaceName name, DayCount dayCount) {
    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Normal expiry-simple moneyness volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent simple moneyness.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param moneynessType  the moneyness type, prices or rates
   * @return the surface metadata
   */
  public static SurfaceMetadata normalVolatilityByExpirySimpleMoneyness(
      String name,
      DayCount dayCount,
      MoneynessType moneynessType) {

    return normalVolatilityByExpirySimpleMoneyness(SurfaceName.of(name), dayCount, moneynessType);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-simple moneyness volatility.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent simple moneyness.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param moneynessType  the moneyness type, prices or rates
   * @return the surface metadata
   */
  public static SurfaceMetadata normalVolatilityByExpirySimpleMoneyness(
      SurfaceName name,
      DayCount dayCount,
      MoneynessType moneynessType) {

    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.SIMPLE_MONEYNESS)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .addInfo(SurfaceInfoType.MONEYNESS_TYPE, moneynessType)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing a SABR expiry-tenor parameter.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param zType  the z-value type, which must be one of the four SABR values
   * @return the surface metadata
   */
  public static SurfaceMetadata sabrParameterByExpiryTenor(
      String name,
      DayCount dayCount,
      ValueType zType) {

    return sabrParameterByExpiryTenor(SurfaceName.of(name), dayCount, zType);
  }

  /**
   * Creates metadata for a surface providing a SABR expiry-tenor parameter.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param zType  the z-value type, which must be one of the four SABR values
   * @return the surface metadata
   */
  public static SurfaceMetadata sabrParameterByExpiryTenor(
      SurfaceName name,
      DayCount dayCount,
      ValueType zType) {

    if (!zType.equals(ValueType.SABR_ALPHA) && !zType.equals(ValueType.SABR_BETA) &&
        !zType.equals(ValueType.SABR_RHO) && !zType.equals(ValueType.SABR_NU)) {
      throw new IllegalArgumentException("SABR z-value type must be SabrAlpha, SabrBeta, SabrRho or SabrNu");
    }
    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(zType)
        .dayCount(dayCount)
        .build();
  }

}
