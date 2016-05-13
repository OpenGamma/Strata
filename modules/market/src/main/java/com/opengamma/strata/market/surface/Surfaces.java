/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

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
   * Creates metadata for a surface providing Black expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param convention  the swap convention
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionBlackExpiryTenor(
      String name,
      DayCount dayCount,
      FixedIborSwapConvention convention) {

    return swaptionBlackExpiryTenor(SurfaceName.of(name), dayCount, convention);
  }

  /**
   * Creates metadata for a surface providing Black expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Black volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param convention  the swap convention
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionBlackExpiryTenor(
      SurfaceName name,
      DayCount dayCount,
      FixedIborSwapConvention convention) {

    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(dayCount)
        .addInfo(SurfaceInfoType.SWAP_CONVENTION, convention)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing Normal expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param convention  the swap convention
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpiryTenor(
      String name,
      DayCount dayCount,
      FixedIborSwapConvention convention) {

    return swaptionNormalExpiryTenor(SurfaceName.of(name), dayCount, convention);
  }

  /**
   * Creates metadata for a surface providing Normal expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * The z-values represent Normal volatility.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param convention  the swap convention
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionNormalExpiryTenor(
      SurfaceName name,
      DayCount dayCount,
      FixedIborSwapConvention convention) {

    return DefaultSurfaceMetadata.builder()
        .surfaceName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .addInfo(SurfaceInfoType.SWAP_CONVENTION, convention)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a surface providing SABR expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param convention  the swap convention
   * @param zType  the z-value type
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionSabrExpiryTenor(
      String name,
      DayCount dayCount,
      FixedIborSwapConvention convention,
      ValueType zType) {

    return swaptionSabrExpiryTenor(SurfaceName.of(name), dayCount, convention, zType);
  }

  /**
   * Creates metadata for a surface providing SABR expiry-tenor volatility for swaptions.
   * <p>
   * The x-values represent expiry year fractions as defined by the specified day count.
   * The y-values represent tenor year fractions, rounded to the month.
   * 
   * @param name  the surface name
   * @param dayCount  the day count
   * @param convention  the swap convention
   * @param zType  the z-value type
   * @return the surface metadata
   */
  public static SurfaceMetadata swaptionSabrExpiryTenor(
      SurfaceName name,
      DayCount dayCount,
      FixedIborSwapConvention convention,
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
        .addInfo(SurfaceInfoType.SWAP_CONVENTION, convention)
        .build();
  }

}
