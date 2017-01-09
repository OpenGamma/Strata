/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link Curves}.
 */
@Test
public class CurvesTest {

  private static final String NAME = "Foo";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);
  private static final List<ParameterMetadata> PARAMS = ImmutableList.of();

  //-------------------------------------------------------------------------
  public void zeroRates_string() {
    CurveMetadata test = Curves.zeroRates(NAME, ACT_360);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  public void zeroRates_curveName() {
    CurveMetadata test = Curves.zeroRates(CURVE_NAME, ACT_360);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  public void zeroRates_curveNameParams() {
    CurveMetadata test = Curves.zeroRates(CURVE_NAME, ACT_360, PARAMS);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_360)
        .parameterMetadata(PARAMS)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void discountFactors_string() {
    CurveMetadata test = Curves.discountFactors(NAME, ACT_360);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  public void discountFactors_curveName() {
    CurveMetadata test = Curves.discountFactors(CURVE_NAME, ACT_360);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  public void discountFactors_curveNameParams() {
    CurveMetadata test = Curves.discountFactors(CURVE_NAME, ACT_360, PARAMS);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_360)
        .parameterMetadata(PARAMS)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void prices_string() {
    CurveMetadata test = Curves.prices(NAME);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.MONTHS)
        .yValueType(ValueType.PRICE_INDEX)
        .build();
    assertEquals(test, expected);
  }

  public void prices_curveName() {
    CurveMetadata test = Curves.prices(CURVE_NAME);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.MONTHS)
        .yValueType(ValueType.PRICE_INDEX)
        .build();
    assertEquals(test, expected);
  }

  public void prices_curveNameParams() {
    CurveMetadata test = Curves.prices(CURVE_NAME, PARAMS);
    CurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.MONTHS)
        .yValueType(ValueType.PRICE_INDEX)
        .parameterMetadata(PARAMS)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(Curves.class);
  }

}
