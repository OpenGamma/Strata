/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Interpolator implementation that returns the log linearly interpolated value.
 * <p>
 * The interpolated value of the function <i>y</i> at <i>x</i> between two data points
 * <i>(x<sub>1</sub>, y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
 * <i>y = y<sub>1</sub> (y<sub>2</sub> / y<sub>1</sub>) ^ ((x - x<sub>1</sub>) /
 * (x<sub>2</sub> - x<sub>1</sub>))</i><br>
 * It is the equivalent of performing a linear interpolation on a data set after
 * taking the logarithm of the y-values.
 */
final class LogLinearCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The interpolator name.
   */
  public static final String NAME = "LogLinear";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new LogLinearCurveInterpolator();

  /**
   * Restricted constructor.
   */
  private LogLinearCurveInterpolator() {
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

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.intervalCount = xValues.size() - 1;
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.intervalCount = base.intervalCount;
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      // x-value is less than the x-value of the last node (lowerIndex < intervalCount)
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      double x1 = xValues[lowerIndex];
      double x2 = xValues[lowerIndex + 1];
      double y1 = yValues[lowerIndex];
      double y2 = yValues[lowerIndex + 1];
      return Math.pow(y2 / y1, (xValue - x1) / (x2 - x1)) * y1;
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      // check if x-value is at the last node
      if (lowerIndex == intervalCount) {
        // if value is at last node, calculate the gradient from the previous interval
        double x1 = xValues[lowerIndex - 1];
        double x2 = xValues[lowerIndex];
        double y1 = yValues[lowerIndex - 1];
        double y2 = yValues[lowerIndex];
        return y2 * Math.log(y2 / y1) / (x2 - x1);
      }
      double x1 = xValues[lowerIndex];
      double x2 = xValues[lowerIndex + 1];
      double y1 = yValues[lowerIndex];
      double y2 = yValues[lowerIndex + 1];
      double yDiv = y2 / y1;
      double xDiff = (x2 - x1);
      return Math.pow(yDiv, (xValue - x1) / xDiff) * y1 * Math.log(yDiv) / xDiff;
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
        double y1 = yValues[lowerIndex];
        double y2 = yValues[lowerIndex + 1];
        double diffInv = 1.0 / (x2 - x1);
        double x1diffInv = (xValue - x1) * diffInv;
        double x2diffInv = (x2 - xValue) * diffInv;
        double yDiv = y1 / y2;
        result[lowerIndex] = Math.pow(yDiv, -x1diffInv) * x2diffInv;
        result[lowerIndex + 1] = Math.pow(yDiv, x2diffInv) * x1diffInv;
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
