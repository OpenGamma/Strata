/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.WeightingFunction;
import com.opengamma.strata.math.impl.interpolation.WeightingFunctions;

/**
 * Interpolator implementation that uses double quadratic interpolation.
 * <p>
 * This uses linear weighting.
 */
final class DoubleQuadraticCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The interpolator name.
   */
  public static final String NAME = "DoubleQuadratic";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new DoubleQuadraticCurveInterpolator();
  /**
   * The weighting function.
   */
  private static final WeightingFunction WEIGHT_FUNCTION = WeightingFunctions.LINEAR;

  /**
   * Restricted constructor.
   */
  private DoubleQuadraticCurveInterpolator() {
  }

  // resolve instance
  private Object readResolve() {
    return INSTANCE;
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public BoundCurveInterpolator bind(DoubleArray xValues, DoubleArray yValues) {
    return new Bound(xValues, yValues);
  }

  //-----------------------------------------------------------------------
  @Override
  public String toString() {
    return NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Bound interpolator.
   */
  static class Bound extends AbstractBoundCurveInterpolator {
    private final double[] xValues;
    private final double[] yValues;
    private final int intervalCount;
    private final RealPolynomialFunction1D[] quadratics;
    private final RealPolynomialFunction1D[] quadraticsFirstDerivative;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.intervalCount = xValues.size() - 1;
      this.quadratics = quadratics(this.xValues, this.yValues, this.intervalCount);
      this.quadraticsFirstDerivative = quadraticsFirstDerivative(this.xValues, this.yValues, this.intervalCount);
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.intervalCount = base.intervalCount;
      this.quadratics = base.quadratics;
      this.quadraticsFirstDerivative = base.quadraticsFirstDerivative;
    }

    //-------------------------------------------------------------------------
    private static RealPolynomialFunction1D[] quadratics(double[] x, double[] y, int intervalCount) {
      if (intervalCount == 1) {
        double a = y[1];
        double b = (y[1] - y[0]) / (x[1] - x[0]);
        return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(a, b)};
      }
      RealPolynomialFunction1D[] quadratic = new RealPolynomialFunction1D[intervalCount - 1];
      for (int i = 1; i < intervalCount; i++) {
        quadratic[i - 1] = quadratic(x, y, i);
      }
      return quadratic;
    }

    private static RealPolynomialFunction1D quadratic(double[] x, double[] y, int index) {
      double a = y[index];
      double dx1 = x[index] - x[index - 1];
      double dx2 = x[index + 1] - x[index];
      double dy1 = y[index] - y[index - 1];
      double dy2 = y[index + 1] - y[index];
      double b = (dx1 * dy2 / dx2 + dx2 * dy1 / dx1) / (dx1 + dx2);
      double c = (dy2 / dx2 - dy1 / dx1) / (dx1 + dx2);
      return new RealPolynomialFunction1D(new double[] {a, b, c});
    }

    private static RealPolynomialFunction1D[] quadraticsFirstDerivative(double[] x, double[] y, int intervalCount) {
      if (intervalCount == 1) {
        double b = (y[1] - y[0]) / (x[1] - x[0]);
        return new RealPolynomialFunction1D[] {new RealPolynomialFunction1D(b)};
      } else {
        RealPolynomialFunction1D[] quadraticFirstDerivative = new RealPolynomialFunction1D[intervalCount - 1];
        for (int i = 1; i < intervalCount; i++) {
          quadraticFirstDerivative[i - 1] = quadraticFirstDerivative(x, y, i);
        }
        return quadraticFirstDerivative;
      }
    }

    private static RealPolynomialFunction1D quadraticFirstDerivative(double[] x, double[] y, int index) {
      double dx1 = x[index] - x[index - 1];
      double dx2 = x[index + 1] - x[index];
      double dy1 = y[index] - y[index - 1];
      double dy2 = y[index + 1] - y[index];
      double b = (dx1 * dy2 / dx2 + dx2 * dy1 / dx1) / (dx1 + dx2);
      double c = (dy2 / dx2 - dy1 / dx1) / (dx1 + dx2);
      return new RealPolynomialFunction1D(new double[] {b, 2. * c});
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      // x-value is less than the x-value of the last node (lowerIndex < intervalCount)
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      int higherIndex = lowerIndex + 1;
      // at start of curve
      if (lowerIndex == 0) {
        RealPolynomialFunction1D quadratic = quadratics[0];
        double x = xValue - xValues[1];
        return quadratic.applyAsDouble(x);
      }
      // at end of curve
      if (higherIndex == intervalCount) {
        RealPolynomialFunction1D quadratic = quadratics[intervalCount - 2];
        double x = xValue - xValues[intervalCount - 1];
        return quadratic.applyAsDouble(x);
      }
      // normal case
      RealPolynomialFunction1D quadratic1 = quadratics[lowerIndex - 1];
      RealPolynomialFunction1D quadratic2 = quadratics[higherIndex - 1];
      double w = WEIGHT_FUNCTION.getWeight((xValues[higherIndex] - xValue) / (xValues[higherIndex] - xValues[lowerIndex]));
      return w * quadratic1.applyAsDouble(xValue - xValues[lowerIndex]) + (1 - w) *
          quadratic2.applyAsDouble(xValue - xValues[higherIndex]);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      int higherIndex = lowerIndex + 1;
      // at start of curve, or only one interval
      if (lowerIndex == 0 || intervalCount == 1) {
        RealPolynomialFunction1D quadraticFirstDerivative = quadraticsFirstDerivative[0];
        double x = xValue - xValues[1];
        return quadraticFirstDerivative.applyAsDouble(x);
      }
      // at end of curve
      if (higherIndex >= intervalCount) {
        RealPolynomialFunction1D quadraticFirstDerivative = quadraticsFirstDerivative[intervalCount - 2];
        double x = xValue - xValues[intervalCount - 1];
        return quadraticFirstDerivative.applyAsDouble(x);
      }
      RealPolynomialFunction1D quadratic1 = quadratics[lowerIndex - 1];
      RealPolynomialFunction1D quadratic2 = quadratics[higherIndex - 1];
      RealPolynomialFunction1D quadratic1FirstDerivative = quadraticsFirstDerivative[lowerIndex - 1];
      RealPolynomialFunction1D quadratic2FirstDerivative = quadraticsFirstDerivative[higherIndex - 1];
      double w = WEIGHT_FUNCTION.getWeight((xValues[higherIndex] - xValue) / (xValues[higherIndex] - xValues[lowerIndex]));
      return w * quadratic1FirstDerivative.applyAsDouble(xValue - xValues[lowerIndex]) +
          (1 - w) * quadratic2FirstDerivative.applyAsDouble(xValue - xValues[higherIndex]) +
          (quadratic2.applyAsDouble(xValue - xValues[higherIndex]) - quadratic1.applyAsDouble(xValue - xValues[lowerIndex])) /
              (xValues[higherIndex] - xValues[lowerIndex]);
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      int higherIndex = lowerIndex + 1;
      int n = xValues.length;
      double[] result = new double[n];
      // at start of curve
      if (lowerIndex == 0) {
        double[] temp = quadraticSensitivities(xValues, xValue, 1);
        result[0] = temp[0];
        result[1] = temp[1];
        result[2] = temp[2];
        return DoubleArray.ofUnsafe(result);
      }
      // at end of curve
      if (higherIndex == intervalCount) {
        double[] temp = quadraticSensitivities(xValues, xValue, n - 2);
        result[n - 3] = temp[0];
        result[n - 2] = temp[1];
        result[n - 1] = temp[2];
        return DoubleArray.ofUnsafe(result);
      }
      // at last node
      if (lowerIndex == intervalCount) {
        result[n - 1] = 1;
        return DoubleArray.ofUnsafe(result);
      }
      double[] temp1 = quadraticSensitivities(xValues, xValue, lowerIndex);
      double[] temp2 = quadraticSensitivities(xValues, xValue, higherIndex);
      double w = WEIGHT_FUNCTION.getWeight((xValues[higherIndex] - xValue) / (xValues[higherIndex] - xValues[lowerIndex]));
      result[lowerIndex - 1] = w * temp1[0];
      result[lowerIndex] = w * temp1[1] + (1 - w) * temp2[0];
      result[higherIndex] = w * temp1[2] + (1 - w) * temp2[1];
      result[higherIndex + 1] = (1 - w) * temp2[2];
      return DoubleArray.ofUnsafe(result);
    }

    private static double[] quadraticSensitivities(double[] xValues, double x, int i) {
      double[] result = new double[3];
      double deltaX = x - xValues[i];
      double h1 = xValues[i] - xValues[i - 1];
      double h2 = xValues[i + 1] - xValues[i];
      result[0] = deltaX * (deltaX - h2) / h1 / (h1 + h2);
      result[1] = 1 + deltaX * (h2 - h1 - deltaX) / h1 / h2;
      result[2] = deltaX * (h1 + deltaX) / (h1 + h2) / h2;
      return result;
    }

    @Override
    public BoundCurveInterpolator bind(
        BoundCurveExtrapolator extrapolatorLeft,
        BoundCurveExtrapolator extrapolatorRight) {

      return new Bound(this, extrapolatorLeft, extrapolatorRight);
    }
  }

}
