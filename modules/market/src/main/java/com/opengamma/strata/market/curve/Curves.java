/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.List;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Helper for creating common types of curves.
 */
public final class Curves {

  /**
   * Restricted constructor.
   */
  private Curves() {
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing zero rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata zeroRates(String name, DayCount dayCount) {
    return zeroRates(CurveName.of(name), dayCount);
  }

  /**
   * Creates curve metadata for a curve providing zero rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata zeroRates(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing zero rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata zeroRates(
      CurveName name,
      DayCount dayCount,
      List<? extends ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing forward rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata forwardRates(String name, DayCount dayCount) {
    return forwardRates(CurveName.of(name), dayCount);
  }

  /**
   * Creates curve metadata for a curve providing forward rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata forwardRates(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.FORWARD_RATE)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing forward rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata forwardRates(
      CurveName name,
      DayCount dayCount,
      List<? extends ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.FORWARD_RATE)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing discount factors.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata discountFactors(String name, DayCount dayCount) {
    return discountFactors(CurveName.of(name), dayCount);
  }

  /**
   * Creates curve metadata for a curve providing discount factors.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata discountFactors(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing discount factors.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata discountFactors(
      CurveName name,
      DayCount dayCount,
      List<? extends ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing monthly prices, typically used in inflation.
   * <p>
   * The x-values represent months relative to an unspecified base month.
   * 
   * @param name  the curve name
   * @return the curve metadata
   */
  public static CurveMetadata prices(String name) {
    return prices(CurveName.of(name));
  }

  /**
   * Creates curve metadata for a curve providing monthly prices, typically used in inflation.
   * <p>
   * The x-values represent months relative to an unspecified base month.
   * 
   * @param name  the curve name
   * @return the curve metadata
   */
  public static CurveMetadata prices(CurveName name) {
    ArgChecker.notNull(name, "name");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.MONTHS)
        .yValueType(ValueType.PRICE_INDEX)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing monthly prices, typically used in inflation.
   * <p>
   * The x-values represent months relative to an unspecified base month.
   * 
   * @param name  the curve name
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata prices(CurveName name, List<? extends ParameterMetadata> parameterMetadata) {
    ArgChecker.notNull(name, "name");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.MONTHS)
        .yValueType(ValueType.PRICE_INDEX)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing Black volatility by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata blackVolatilityByExpiry(String name, DayCount dayCount) {
    return blackVolatilityByExpiry(CurveName.of(name), dayCount);
  }

  /**
   * Creates curve metadata for a curve providing Black volatility by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata blackVolatilityByExpiry(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing Black volatility by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata blackVolatilityByExpiry(
      CurveName name,
      DayCount dayCount,
      List<? extends ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing normal volatility by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata normalVolatilityByExpiry(String name, DayCount dayCount) {
    return normalVolatilityByExpiry(CurveName.of(name), dayCount);
  }

  /**
   * Creates curve metadata for a curve providing normal volatility by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata normalVolatilityByExpiry(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing normal volatility by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata normalVolatilityByExpiry(
      CurveName name,
      DayCount dayCount,
      List<? extends ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing recovery rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata recoveryRates(String name, DayCount dayCount) {
    return recoveryRates(CurveName.of(name), dayCount);
  }

  /**
   * Creates curve metadata for a curve providing recovery rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata recoveryRates(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.RECOVERY_RATE)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing recovery rates.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata recoveryRates(
      CurveName name,
      DayCount dayCount,
      List<? extends ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.RECOVERY_RATE)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates metadata for a curve providing a SABR parameter.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param yType  the y-value type, which must be one of the four SABR values
   * @return the curve metadata
   */
  public static CurveMetadata sabrParameterByExpiry(
      String name,
      DayCount dayCount,
      ValueType yType) {

    return sabrParameterByExpiry(CurveName.of(name), dayCount, yType);
  }

  /**
   * Creates metadata for a curve providing a SABR parameter.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param yType  the y-value type, which must be one of the four SABR values
   * @return the curve metadata
   */
  public static CurveMetadata sabrParameterByExpiry(
      CurveName name,
      DayCount dayCount,
      ValueType yType) {

    if (!yType.equals(ValueType.SABR_ALPHA) && !yType.equals(ValueType.SABR_BETA) &&
        !yType.equals(ValueType.SABR_RHO) && !yType.equals(ValueType.SABR_NU)) {
      throw new IllegalArgumentException("SABR y-value type must be SabrAlpha, SabrBeta, SabrRho or SabrNu");
    }
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(yType)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates metadata for a curve providing a SABR parameter.
   * <p>
   * The x-values represent time to expiry year fractions as defined by the specified day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count
   * @param yType  the y-value type, which must be one of the four SABR values
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata sabrParameterByExpiry(
      CurveName name,
      DayCount dayCount,
      ValueType yType,
      List<? extends ParameterMetadata> parameterMetadata) {

    if (!yType.equals(ValueType.SABR_ALPHA) && !yType.equals(ValueType.SABR_BETA) &&
        !yType.equals(ValueType.SABR_RHO) && !yType.equals(ValueType.SABR_NU)) {
      throw new IllegalArgumentException("SABR y-value type must be SabrAlpha, SabrBeta, SabrRho or SabrNu");
    }
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(yType)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates curve metadata for a curve providing correlation by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   *
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata correlationByExpiry(String name, DayCount dayCount) {
    return correlationByExpiry(CurveName.of(name), dayCount);
  }

  /**
   * Creates curve metadata for a curve providing correlation by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   *
   * @param name  the curve name
   * @param dayCount  the day count
   * @return the curve metadata
   */
  public static CurveMetadata correlationByExpiry(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.CORRELATION)
        .dayCount(dayCount)
        .build();
  }

  /**
   * Creates curve metadata for a curve providing correlation by expiry.
   * <p>
   * The x-values represent year fractions relative to an unspecified base date
   * as defined by the specified day count.
   *
   * @param name  the curve name
   * @param dayCount  the day count
   * @param parameterMetadata  the parameter metadata
   * @return the curve metadata
   */
  public static CurveMetadata correlationByExpiry(
      CurveName name,
      DayCount dayCount,
      List<? extends ParameterMetadata> parameterMetadata) {

    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.CORRELATION)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

}
