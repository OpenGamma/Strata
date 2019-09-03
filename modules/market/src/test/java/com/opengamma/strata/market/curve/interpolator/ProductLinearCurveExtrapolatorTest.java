/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;

/**
 * Test {@link ProductLinearCurveExtrapolator}.
 */
public class ProductLinearCurveExtrapolatorTest {

  private static final CurveExtrapolator EXTRAP = ProductLinearCurveExtrapolator.INSTANCE;
  private static final DoubleArray X_DATA = DoubleArray.of(-0.5, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final int SIZE = X_DATA.size();
  private static final DoubleArray X_LEFT_TEST = DoubleArray.of(-3.5, -1.1);
  private static final DoubleArray X_RIGHT_TEST = DoubleArray.of(8.5, 11.1);
  private static final double EPS = 1.e-7;
  private static final ScalarFirstOrderDifferentiator DIFF_CALC =
      new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFieldFirstOrderDifferentiator SENS_CALC =
      new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);

  @Test
  public void test_basics() {
    assertThat(EXTRAP.getName()).isEqualTo(ProductLinearCurveExtrapolator.NAME);
    assertThat(EXTRAP.toString()).isEqualTo(ProductLinearCurveExtrapolator.NAME);
  }

  @Test
  public void test_extrapolation() {
    BoundCurveInterpolator bind =
        CurveInterpolators.DOUBLE_QUADRATIC.bind(X_DATA, Y_DATA, EXTRAP, EXTRAP);
    double gradLeft =
        (bind.interpolate(X_DATA.get(0) + EPS) * (X_DATA.get(0) + EPS) - Y_DATA.get(0) * X_DATA.get(0)) / EPS;
    for (int i = 0; i < X_LEFT_TEST.size(); ++i) {
      double xyLeft = gradLeft * (X_LEFT_TEST.get(i) - X_DATA.get(0)) + Y_DATA.get(0) * X_DATA.get(0);
      double expected = xyLeft / X_LEFT_TEST.get(i);
      assertThat(bind.interpolate(X_LEFT_TEST.get(i))).isCloseTo(expected, offset(10d * Math.abs(expected) * EPS));
    }
    double gradRight = (Y_DATA.get(SIZE - 1) * X_DATA.get(SIZE - 1) -
        bind.interpolate(X_DATA.get(SIZE - 1) - EPS) * (X_DATA.get(SIZE - 1) - EPS)) / EPS;
    for (int i = 0; i < X_RIGHT_TEST.size(); ++i) {
      double xyRight = gradRight * (X_RIGHT_TEST.get(i) - X_DATA.get(SIZE - 1)) +
          Y_DATA.get(SIZE - 1) * X_DATA.get(SIZE - 1);
      double expected = xyRight / X_RIGHT_TEST.get(i);
      assertThat(bind.interpolate(X_RIGHT_TEST.get(i))).isCloseTo(expected, offset(10d * Math.abs(expected) * EPS));
    }
  }

  @Test
  public void test_derivative_sensitivity() {
    BoundCurveInterpolator bind =
        CurveInterpolators.DOUBLE_QUADRATIC.bind(X_DATA, Y_DATA, EXTRAP, EXTRAP);
    Function<Double, Double> derivFunc = x -> bind.interpolate(x);

    for (int i = 0; i < X_LEFT_TEST.size(); ++i) {
      assertThat(bind.firstDerivative(X_LEFT_TEST.get(i)))
          .isCloseTo(DIFF_CALC.differentiate(derivFunc).apply(X_LEFT_TEST.get(i)), offset(EPS));
      int index = i;
      Function<DoubleArray, Double> sensFunc =
          y -> CurveInterpolators.DOUBLE_QUADRATIC.bind(X_DATA, y, EXTRAP, EXTRAP).interpolate(X_LEFT_TEST.get(index));
      assertThat(DoubleArrayMath.fuzzyEquals(bind.parameterSensitivity(X_LEFT_TEST.get(index)).toArray(),
          SENS_CALC.differentiate(sensFunc).apply(Y_DATA).toArray(), EPS)).isTrue();
    }
    for (int i = 0; i < X_RIGHT_TEST.size(); ++i) {
      assertThat(bind.firstDerivative(X_RIGHT_TEST.get(i)))
          .isCloseTo(DIFF_CALC.differentiate(derivFunc).apply(X_RIGHT_TEST.get(i)), offset(EPS));
      int index = i;
      Function<DoubleArray, Double> sensFunc =
          y -> CurveInterpolators.DOUBLE_QUADRATIC.bind(X_DATA, y, EXTRAP, EXTRAP).interpolate(X_RIGHT_TEST.get(index));
      assertThat(DoubleArrayMath.fuzzyEquals(bind.parameterSensitivity(X_RIGHT_TEST.get(index)).toArray(),
          SENS_CALC.differentiate(sensFunc).apply(Y_DATA).toArray(), EPS)).isTrue();
    }
  }

  @Test
  public void errorTest() {
    DoubleArray xValues1 = DoubleArray.of(1, 2, 3);
    DoubleArray xValues2 = DoubleArray.of(-3, -2, -1);
    DoubleArray yValues = DoubleArray.of(1, 2, 3);
    BoundCurveInterpolator bind1 =
        CurveInterpolators.DOUBLE_QUADRATIC.bind(xValues1, yValues, EXTRAP, EXTRAP);
    BoundCurveInterpolator bind2 =
        CurveInterpolators.DOUBLE_QUADRATIC.bind(xValues2, yValues, EXTRAP, EXTRAP);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bind1.interpolate(-1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bind1.firstDerivative(-1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bind1.parameterSensitivity(-1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bind2.interpolate(1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bind2.firstDerivative(1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bind2.parameterSensitivity(1));
  }

  @Test
  public void test_serialization() {
    assertSerialization(EXTRAP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void sampleDataTest() {
    DoubleArray xValues = DoubleArray.of(0.5, 1.0, 5.0, 10.0);
    DoubleArray yValues = DoubleArray.of(0.02, 0.05, 0.015, 0.01);
    DoubleArray rightKeys = DoubleArray.of(10.0, 12.0, 25.0, 35.0);
    DoubleArray leftKeys = DoubleArray.of(0.5, 0.25, 0.12, 0.005);
    BoundCurveInterpolator bind = CurveInterpolators.PRODUCT_NATURAL_SPLINE
        .bind(xValues, yValues, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    Function<Double, Double> fwdFunc = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0.5 * (bind.interpolate(x + EPS) * (x + EPS) - bind.interpolate(x - EPS) * (x - EPS)) / EPS;
      }
    };
    for (int i = 1; i < 3; ++i) {
      // constant forward
      assertThat(fwdFunc.apply(rightKeys.get(0))).isCloseTo(fwdFunc.apply(rightKeys.get(i)), offset(EPS));
      // constant zero
      assertThat(bind.interpolate(leftKeys.get(0))).isCloseTo(bind.interpolate(leftKeys.get(i)), offset(EPS));
    }
  }

}
