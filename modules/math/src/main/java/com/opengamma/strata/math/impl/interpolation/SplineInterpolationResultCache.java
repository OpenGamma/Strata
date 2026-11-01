/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * A single immutable snapshot of an interpolation, without node sensitivities or a matrix decomposition.
 */
final class SplineInterpolationResultCache {

  private final double[] xValues;
  private final double[] yValues;
  private final PiecewisePolynomialResult result;

  SplineInterpolationResultCache(double[] xValues, double[] yValues, PiecewisePolynomialResult result) {
    // Arrays are private copies owned by the interpolation invocation and are never exposed.
    this.xValues = xValues;
    this.yValues = yValues;
    this.result = result;
  }

  boolean matches(double[] xValues, double[] yValues) {
    return Arrays.equals(this.xValues, xValues) && Arrays.equals(this.yValues, yValues);
  }

  PiecewisePolynomialResultsWithSensitivity withSensitivity(DoubleMatrix[] sensitivities) {
    for (DoubleMatrix sensitivity : sensitivities) {
      for (int row = 0; row < sensitivity.rowCount(); row++) {
        for (int col = 0; col < sensitivity.columnCount(); col++) {
          ArgChecker.isTrue(Doubles.isFinite(sensitivity.get(row, col)), "Matrix contains a NaN or infinite");
        }
      }
    }
    return new PiecewisePolynomialResultsWithSensitivity(
        result.getKnots(), result.getCoefMatrix(), result.getOrder(), result.getDimensions(), sensitivities);
  }
}
