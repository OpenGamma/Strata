/**
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
  @SuppressWarnings("unchecked")
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
        .parameterMetadata((List<ParameterMetadata>) parameterMetadata)
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
  @SuppressWarnings("unchecked")
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
        .parameterMetadata((List<ParameterMetadata>) parameterMetadata)
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
  @SuppressWarnings("unchecked")
  public static CurveMetadata prices(CurveName name, List<? extends ParameterMetadata> parameterMetadata) {
    ArgChecker.notNull(name, "name");
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(ValueType.MONTHS)
        .yValueType(ValueType.PRICE_INDEX)
        .parameterMetadata((List<ParameterMetadata>) parameterMetadata)
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

}
