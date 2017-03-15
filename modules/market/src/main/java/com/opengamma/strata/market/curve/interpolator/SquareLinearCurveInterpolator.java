/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * The interpolation is linear on y^2. The interpolator is used for interpolation on variance for options.
 * All values of y must be positive.
 */
final class SquareLinearCurveInterpolator implements CurveInterpolator, Serializable {

  /**
   * The interpolator name.
   */
  public static final String NAME = "SquareLinear";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new SquareLinearCurveInterpolator();
  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Level below which the value is consider to be 0.
   */
  private static final double EPS = 1.0E-10;

  /**
   * Restricted constructor.
   */
  private SquareLinearCurveInterpolator() {
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
    private final int dataSize;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.dataSize = xValues.size();
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.dataSize = xValues.length;
    }

    //-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      // x-value is less than the x-value of the last node (lowerIndex < intervalCount)
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      double x1 = xValues[lowerIndex];
      double y1 = yValues[lowerIndex];

      int higherIndex = lowerIndex + 1;
      double x2 = xValues[higherIndex];
      double y2 = yValues[higherIndex];

      double w = (x2 - xValue) / (x2 - x1);
      double y21 = y1 * y1;
      double y22 = y2 * y2;
      double ySq = w * y21 + (1.0 - w) * y22;
      return Math.sqrt(ySq);
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      int index;
      // check if x-value is at the last node
      if (lowerIndex == dataSize - 1) {
        index = dataSize - 2;
      } else {
        index = lowerIndex;
      }

      double x1 = xValues[index];
      double y1 = yValues[index];
      double x2 = xValues[index + 1];
      double y2 = yValues[index + 1];

      if ((y1 < EPS) && (y2 >= EPS) && (xValue - x1) < EPS) { // On one vertex with value 0, other vertex not 0
        throw new IllegalArgumentException("ask for first derivative on a value without derivative; value " + xValue +
            " is close to vertex " + x1 + " and value at vertex is " + y1);
      }
      if ((y2 < EPS) && (y1 >= EPS) && (x2 - xValue) < EPS) { // On one vertex with value 0, other vertex not 0
        throw new IllegalArgumentException("ask for first derivative on a value without derivative; value " + xValue +
            " is close to vertex " + x2 + " and value at vertex is " + y2);
      }
      if ((y1 < EPS) && (y2 < EPS)) { // Both vertices have 0 value, return 0.
        return 0.0;
      }

      double w = (x2 - xValue) / (x2 - x1);
      double y21 = y1 * y1;
      double y22 = y2 * y2;
      double ySq = w * y21 + (1.0 - w) * y22;
      return 0.5 * (y22 - y21) / (x2 - x1) / Math.sqrt(ySq);
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      double[] result = new double[dataSize];

      int lowerIndex = lowerBoundIndex(xValue, xValues);
      double x1 = xValues[lowerIndex];
      double y1 = yValues[lowerIndex];
      // check if x-value is at the last node
      if (lowerIndex == dataSize - 1) {
        result[dataSize - 1] = 1.0;
        return DoubleArray.ofUnsafe(result);
      }

      int higherIndex = lowerIndex + 1;
      double x2 = xValues[higherIndex];
      double y2 = yValues[higherIndex];
      if ((xValue - x1) < EPS) { // On or very close to Vertex 1
        result[lowerIndex] = 1.0d;
        return DoubleArray.ofUnsafe(result);
      }
      if ((x2 - xValue) < EPS) { // On or very close to Vertex 2
        result[lowerIndex + 1] = 1.0d;
        return DoubleArray.ofUnsafe(result);
      }
      double w2 = (x2 - xValue) / (x2 - x1);
      if ((y2 < EPS) && (y1 < EPS)) { // Both values very close to 0
        result[lowerIndex] = Math.sqrt(w2);
        result[lowerIndex + 1] = Math.sqrt(1.0d - w2);
        return DoubleArray.ofUnsafe(result);
      }

      double y21 = y1 * y1;
      double y22 = y2 * y2;
      double ySq = w2 * y21 + (1.0 - w2) * y22;
      // Backward
      double ySqBar = 0.5 / Math.sqrt(ySq);
      double y22Bar = (1.0 - w2) * ySqBar;
      double y21Bar = w2 * ySqBar;
      double y1Bar = 2 * y1 * y21Bar;
      double y2Bar = 2 * y2 * y22Bar;
      result[lowerIndex] = y1Bar;
      result[lowerIndex + 1] = y2Bar;

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
