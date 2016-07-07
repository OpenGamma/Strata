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
   * Creates metadata for a surface providing Black expiry-strike volatility for Ibor caplet/floorlet.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata iborCapletFloorletBlackExpiryStrike(String name, DayCount dayCount) {
    return iborCapletFloorletBlackExpiryStrike(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Black expiry-strike volatility for Ibor caplet/floorlet.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata iborCapletFloorletBlackExpiryStrike(SurfaceName name, DayCount dayCount) {
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
   * Creates metadata for a surface providing Normal expiry-strike volatility for Ibor caplet/floorlet.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata iborCapletFloorletNormalExpiryStrike(String name, DayCount dayCount) {
    return iborCapletFloorletNormalExpiryStrike(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-strike volatility for Ibor caplet/floorlet.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata iborCapletFloorletNormalExpiryStrike(SurfaceName name, DayCount dayCount) {
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
   * Creates metadata for a surface providing Normal expiry-tenor volatility for swaptions.
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
  public static SurfaceMetadata iborFutureOptionNormalExpirySimpleMoneyness(
      String name,
      DayCount dayCount,
      MoneynessType moneynessType) {

    return iborFutureOptionNormalExpirySimpleMoneyness(SurfaceName.of(name), dayCount, moneynessType);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-tenor volatility for swaptions.
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
  public static SurfaceMetadata iborFutureOptionNormalExpirySimpleMoneyness(
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
   * Creates metadata for a surface providing Black expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionBlackExpiryTenor(String name, DayCount dayCount) {
    return swaptionBlackExpiryTenor(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Black expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionBlackExpiryTenor(SurfaceName name, DayCount dayCount) {
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
   * Creates metadata for a surface providing Normal expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpiryTenor(String name, DayCount dayCount) {
    return swaptionNormalExpiryTenor(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpiryTenor(SurfaceName name, DayCount dayCount) {
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
   * Creates metadata for a surface providing Normal expiry-simple moneyness volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent simple moneyness (strike - forward).
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpirySimpleMoneyness(String name, DayCount dayCount) {
    return swaptionNormalExpirySimpleMoneyness(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-simple moneyness volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent simple moneyness (strike - forward).
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpirySimpleMoneyness(SurfaceName name, DayCount dayCount) {
    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.SIMPLE_MONEYNESS)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Normal expiry-strike volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpiryStrike(String name, DayCount dayCount) {
    return swaptionNormalExpiryStrike(SurfaceName.of(name), dayCount);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-strike volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent strike.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpiryStrike(SurfaceName name, DayCount dayCount) {
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
   * Creates metadata for a surface providing SABR expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param zType  the z-value type
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionSabrExpiryTenor(
      String name,
      DayCount dayCount,
      ValueType zType) {

    return swaptionSabrExpiryTenor(SurfaceName.of(name), dayCount, zType);
  }

  /**
   * Creates metadata for a surface providing SABR expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param zType  the z-value type
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionSabrExpiryTenor(
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
