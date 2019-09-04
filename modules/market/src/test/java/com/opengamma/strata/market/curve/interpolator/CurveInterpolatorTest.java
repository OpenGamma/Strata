/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LOG_LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LOG_NATURAL_SPLINE_DISCOUNT_FACTOR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_CUBIC_SPLINE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE_NONNEGATIVITY_CUBIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PRODUCT_NATURAL_SPLINE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.SQUARE_LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.TIME_SQUARE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link CurveInterpolator}.
 */
public class CurveInterpolatorTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {LINEAR, "Linear"},
        {LOG_LINEAR, "LogLinear"},
        {SQUARE_LINEAR, "SquareLinear"},
        {DOUBLE_QUADRATIC, "DoubleQuadratic"},
        {TIME_SQUARE, "TimeSquare"},
        {LOG_NATURAL_SPLINE_MONOTONE_CUBIC, "LogNaturalSplineMonotoneCubic"},
        {LOG_NATURAL_SPLINE_DISCOUNT_FACTOR, "LogNaturalSplineDiscountFactor"},
        {NATURAL_CUBIC_SPLINE, "NaturalCubicSpline"},
        {NATURAL_SPLINE, "NaturalSpline"},
        {NATURAL_SPLINE_NONNEGATIVITY_CUBIC, "NaturalSplineNonnegativityCubic"},
        {PRODUCT_NATURAL_SPLINE, "ProductNaturalSpline"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(CurveInterpolator convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CurveInterpolator convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CurveInterpolator convention, String name) {
    assertThat(CurveInterpolator.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(CurveInterpolator convention, String name) {
    ImmutableMap<String, CurveInterpolator> map = CurveInterpolator.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveInterpolator.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveInterpolator.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_bind() {
    DoubleArray xValues = DoubleArray.of(1, 2, 3);
    DoubleArray yValues = DoubleArray.of(2, 4, 5);
    BoundCurveInterpolator bound = LINEAR.bind(xValues, yValues, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
    assertThat(bound.interpolate(0.5)).isCloseTo(2d, offset(0d));
    assertThat(bound.interpolate(1)).isCloseTo(2d, offset(0d));
    assertThat(bound.interpolate(1.5)).isCloseTo(3d, offset(0d));
    assertThat(bound.interpolate(2)).isCloseTo(4d, offset(0d));
    assertThat(bound.interpolate(2.5)).isCloseTo(4.5d, offset(0d));
    assertThat(bound.interpolate(3)).isCloseTo(5d, offset(0d));
    assertThat(bound.interpolate(3.5)).isCloseTo(5d, offset(0d));
    // coverage
    assertThat(bound.parameterSensitivity(0.5).size()).isEqualTo(3);
    assertThat(bound.parameterSensitivity(2).size()).isEqualTo(3);
    assertThat(bound.parameterSensitivity(3.5).size()).isEqualTo(3);
    assertThat(bound.firstDerivative(0.5)).isCloseTo(0d, offset(0d));
    assertThat(bound.firstDerivative(2) != 0d).isTrue();
    assertThat(bound.firstDerivative(3.5)).isCloseTo(0d, offset(0d));
    assertThat(bound.toString()).isNotNull();
  }

  @Test
  public void test_lowerBound() {
    // bad input, but still produces good output
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(0.0d, new double[] {1, 2, 3})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(0.5d, new double[] {1, 2, 3})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(0.9999d, new double[] {1, 2, 3})).isEqualTo(0);
    // good input
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(1.0d, new double[] {1, 2, 3})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(1.0001d, new double[] {1, 2, 3})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(1.9999d, new double[] {1, 2, 3})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(2.0d, new double[] {1, 2, 3})).isEqualTo(1);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(2.0001d, new double[] {1, 2, 3})).isEqualTo(1);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(2.9999d, new double[] {1, 2, 3})).isEqualTo(1);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(3.0d, new double[] {1, 2, 3})).isEqualTo(2);
    // bad input, but still produces good output
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(3.0001d, new double[] {1, 2, 3})).isEqualTo(2);
    // check zero
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(-1.0d, new double[] {-1, 0, 1})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(-0.9999d, new double[] {-1, 0, 1})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(-0.0001d, new double[] {-1, 0, 1})).isEqualTo(0);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(-0.0d, new double[] {-1, 0, 1})).isEqualTo(1);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(0.0d, new double[] {-1, 0, 1})).isEqualTo(1);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(1.0d, new double[] {-1, 0, 1})).isEqualTo(2);
    assertThat(AbstractBoundCurveInterpolator.lowerBoundIndex(1.5d, new double[] {-1, 0, 1})).isEqualTo(2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(CurveInterpolators.class);
    coverPrivateConstructor(StandardCurveInterpolators.class);
    assertThat(LINEAR.equals(null)).isFalse();
    assertThat(LINEAR.equals(ANOTHER_TYPE)).isFalse();
  }

  @Test
  public void test_serialization() {
    assertSerialization(LINEAR);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CurveInterpolator.class, LINEAR);
    assertJodaConvert(CurveInterpolator.class, LOG_LINEAR);
  }

}
