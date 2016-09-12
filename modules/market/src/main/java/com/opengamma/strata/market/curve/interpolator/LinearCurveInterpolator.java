/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Interpolator implementation that returns the linearly interpolated value.
 * <p>
 * The interpolated value of the function <i>y</i> at <i>x</i> between two data points
 * <i>(x<sub>1</sub>, y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
 * <i>y = y<sub>1</sub> + (x - x<sub>1</sub>) * (y<sub>2</sub> - y<sub>1</sub>)
 * / (x<sub>2</sub> - x<sub>1</sub>)</i>.
 */
final class LinearCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The interpolator name.
   */
  public static final String NAME = "Linear";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new LinearCurveInterpolator();

  /**
   * Restricted constructor.
   */
  private LinearCurveInterpolator() {
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
    private final double[] gradients;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.intervalCount = xValues.size() - 1;
      this.gradients = new double[intervalCount];
      for (int i = 0; i < intervalCount; i++) {
        double x1 = xValues.get(i);
        double y1 = yValues.get(i);
        double x2 = xValues.get(i + 1);
        double y2 = yValues.get(i + 1);
        double gradient = (y2 - y1) / (x2 - x1);
        this.gradients[i] = gradient;
      }
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.intervalCount = base.intervalCount;
      this.gradients = base.gradients;
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      // x-value is less than the x-value of the last node (lowerIndex < intervalCount)
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      double x1 = xValues[lowerIndex];
      double y1 = yValues[lowerIndex];
      return y1 + (xValue - x1) * gradients[lowerIndex];
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      // check if x-value is at the last node
      if (lowerIndex == intervalCount) {
        // if value is at last node, calculate the gradient from the previous interval
        lowerIndex--;
      }
      return gradients[lowerIndex];
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      double[] result = new double[yValues.length];
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      // check if x-value is at the last node
      if (lowerIndex == intervalCount) {
        // sensitivity is entirely to the last node
        result[intervalCount] = 1d;
      } else {
        double x1 = xValues[lowerIndex];
        double x2 = xValues[lowerIndex + 1];
        double dx = x2 - x1;
        double a = (x2 - xValue) / dx;
        result[lowerIndex] = a;
        result[lowerIndex + 1] = 1 - a;
      }
      return DoubleArray.ofUnsafe(result);
    }

    @Override
    public BoundCurveInterpolator bind(
        BoundCurveExtrapolator extrapolatorLeft,
        BoundCurveExtrapolator extrapolatorRight) {

      return new Bound(this, extrapolatorLeft, extrapolatorRight);
    }
  }

}
