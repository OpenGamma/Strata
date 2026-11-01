/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Tests bounded coefficient reuse without changing numerical results or custom solver behavior.
 */
public class SplineInterpolationResultCacheTest {

  @Test
  public void test_natural() {
    checkReuse(true, false);
  }

  @Test
  public void test_notAKnot() {
    checkReuse(false, false);
  }

  @Test
  public void test_clamped() {
    checkReuse(false, true);
  }

  private void checkReuse(boolean natural, boolean clamped) {
    for (int size : new int[] {2, 3, 8}) {
      double[] x = xValues(size);
      double[] y = yValues(size, clamped);
      PiecewisePolynomialInterpolator interpolator = method(natural);
      PiecewisePolynomialResult value = interpolator.interpolate(x, y);
      if (!natural && !clamped && size == 2) {
        // The uncached upstream path rejects the two-point result shape. A cache hit must not conceal that error.
        assertThatIllegalArgumentException().isThrownBy(() -> method(false).interpolateWithSensitivity(x, y));
        assertThatIllegalArgumentException().isThrownBy(() -> interpolator.interpolateWithSensitivity(x, y));
        continue;
      }
      PiecewisePolynomialResultsWithSensitivity expected = method(natural).interpolateWithSensitivity(x, y);
      PiecewisePolynomialResultsWithSensitivity actual = interpolator.interpolateWithSensitivity(x, y);
      assertThat(actual).as("size %s", size).isEqualTo(expected);
      assertThat(actual.getCoefMatrix()).as("reuse coefficients for size %s", size).isSameAs(value.getCoefMatrix());
      assertThat(interpolator.interpolateWithSensitivity(x, y)).isEqualTo(expected);
      int offset = clamped ? 1 : 0;
      for (int node = 0; node < size; node++) {
        double shift = 1e-6;
        double[] up = y.clone();
        double[] down = y.clone();
        up[node + offset] += shift;
        down[node + offset] -= shift;
        DoubleMatrix upCoefficients = method(natural).interpolate(x, up).getCoefMatrix();
        DoubleMatrix downCoefficients = method(natural).interpolate(x, down).getCoefMatrix();
        for (int interval = 0; interval < value.getNumberOfIntervals(); interval++) {
          DoubleMatrix sensitivity = actual.getCoefficientSensitivity(interval);
          for (int coefficient = 0; coefficient < value.getOrder(); coefficient++) {
            double finiteDifference =
                (upCoefficients.get(interval, coefficient) - downCoefficients.get(interval, coefficient)) / (2 * shift);
            assertThat(sensitivity.get(coefficient, node)).isCloseTo(
                finiteDifference, org.assertj.core.data.Offset.offset(1e-7));
          }
        }
      }
    }
  }

  @Test
  public void test_mutatedInputsAndBoundaryConditions() {
    for (int kind = 0; kind < 3; kind++) {
      boolean natural = kind == 0;
      boolean clamped = kind == 2;
      for (int change = 0; change < (clamped ? 4 : 2); change++) {
        double[] x = xValues(8);
        double[] y = yValues(8, clamped);
        PiecewisePolynomialInterpolator interpolator = method(natural);
        PiecewisePolynomialResult old = interpolator.interpolate(x, y);
        if (change == 0) {
          x[1] += 0.1;
        } else if (change == 1) {
          y[clamped ? 2 : 1] += 0.1;
        } else if (change == 2) {
          y[0] += 0.1;
        } else {
          y[y.length - 1] += 0.1;
        }
        PiecewisePolynomialResultsWithSensitivity actual = interpolator.interpolateWithSensitivity(x, y);
        assertThat(actual).isEqualTo(method(natural).interpolateWithSensitivity(x, y));
        assertThat(actual.getCoefMatrix()).isNotSameAs(old.getCoefMatrix());
      }
    }
  }

  @Test
  public void test_onlyLatestOrderedOneDimensionalResult() {
    for (boolean natural : new boolean[] {false, true}) {
      double[] x = xValues(8);
      double[] firstY = yValues(8, false);
      double[] secondY = firstY.clone();
      secondY[1] += 1;
      PiecewisePolynomialInterpolator interpolator = method(natural);
      PiecewisePolynomialResult first = interpolator.interpolate(x, firstY);
      PiecewisePolynomialResult second = interpolator.interpolate(x, secondY);
      assertThat(interpolator.interpolateWithSensitivity(x, firstY).getCoefMatrix()).isNotSameAs(first.getCoefMatrix());
      assertThat(interpolator.interpolateWithSensitivity(x, secondY).getCoefMatrix()).isSameAs(second.getCoefMatrix());
      double[] unsortedX = {2, 0, 1, 3};
      double[] unsortedY = {4, 1, 2, 5};
      interpolator.interpolate(unsortedX, unsortedY);
      assertThat(interpolator.interpolateWithSensitivity(unsortedX, unsortedY))
          .isEqualTo(method(natural).interpolateWithSensitivity(unsortedX, unsortedY));
      interpolator.interpolate(x, firstY);
      interpolator.interpolate(x, new double[][] {firstY, secondY});
      assertThat(interpolator.interpolateWithSensitivity(x, firstY))
          .isEqualTo(method(natural).interpolateWithSensitivity(x, firstY));
    }
  }

  @Test
  public void test_customNaturalSolverHookPreserved() {
    int[] calls = {0};
    CubicSplineNaturalSolver solver = new CubicSplineNaturalSolver() {
      @Override
      public DoubleMatrix[] solveWithSensitivity(double[] xValues, double[] yValues) {
        calls[0]++;
        return super.solveWithSensitivity(xValues, yValues);
      }
    };
    NaturalSplineInterpolator interpolator = new NaturalSplineInterpolator(solver);
    double[] x = xValues(8);
    double[] y = yValues(8, false);
    PiecewisePolynomialResult value = interpolator.interpolate(x, y);
    PiecewisePolynomialResultsWithSensitivity actual = interpolator.interpolateWithSensitivity(x, y);
    assertThat(calls[0]).isEqualTo(1);
    assertThat(actual).isEqualTo(new NaturalSplineInterpolator().interpolateWithSensitivity(x, y));
    assertThat(actual.getCoefMatrix()).isNotSameAs(value.getCoefMatrix());
  }

  @Test
  public void test_invalidInputsStillRejected() {
    for (boolean natural : new boolean[] {false, true}) {
      PiecewisePolynomialInterpolator interpolator = method(natural);
      double[] x = xValues(8);
      double[] y = yValues(8, false);
      interpolator.interpolate(x, y);
      y[1] = Double.NaN;
      assertThatIllegalArgumentException().isThrownBy(() -> interpolator.interpolateWithSensitivity(x, y));
      y[1] = Double.POSITIVE_INFINITY;
      assertThatIllegalArgumentException().isThrownBy(() -> interpolator.interpolateWithSensitivity(x, y));
      y[1] = 2;
      x[1] = x[0];
      assertThatIllegalArgumentException().isThrownBy(() -> interpolator.interpolateWithSensitivity(x, y));
    }
  }

  @Test
  public void test_concurrentBoundaryConditions() throws Exception {
    CubicSplineInterpolator interpolator = new CubicSplineInterpolator();
    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      List<Callable<Boolean>> tasks = new ArrayList<>();
      for (int task = 0; task < 4; task++) {
        final boolean clamped = task % 2 == 0;
        final double adjustment = task;
        tasks.add(() -> {
          double[] x = xValues(8);
          double[] y = yValues(8, clamped);
          y[2] += adjustment;
          PiecewisePolynomialResultsWithSensitivity expected = new CubicSplineInterpolator().interpolateWithSensitivity(x, y);
          for (int repeat = 0; repeat < 100; repeat++) {
            interpolator.interpolate(x, y);
            if (!interpolator.interpolateWithSensitivity(x, y).equals(expected)) {
              return false;
            }
          }
          return true;
        });
      }
      for (Future<Boolean> future : executor.invokeAll(tasks)) {
        assertThat(future.get(20, TimeUnit.SECONDS)).isTrue();
      }
    } finally {
      executor.shutdownNow();
    }
  }

  private static PiecewisePolynomialInterpolator method(boolean natural) {
    return natural ? new NaturalSplineInterpolator() : new CubicSplineInterpolator();
  }

  private static double[] xValues(int size) {
    double[] x = new double[size];
    for (int i = 0; i < size; i++) {
      x[i] = i + i * i * 0.1;
    }
    return x;
  }

  private static double[] yValues(int size, boolean clamped) {
    double[] y = new double[size + (clamped ? 2 : 0)];
    for (int i = 0; i < size; i++) {
      y[i + (clamped ? 1 : 0)] = 1 + Math.sin(i) + i * 0.2;
    }
    if (clamped) {
      y[0] = 0.25;
      y[y.length - 1] = -0.5;
    }
    return y;
  }
}
