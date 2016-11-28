/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Interpolator implementation that uses upper step interpolation.
 * <p>
 * The interpolated value at <i>x</i> s.t. <i>x<sub>1</sub> < x =< x<sub>2</sub></i> is the value at <i>x<sub>2</sub></i>. 
 * The flat extrapolation is implemented outside the data range.
 */
final class StepUpperCurveInterpolator
    implements CurveInterpolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The small parameter.
   * <p>
   * A value will be treated as 0 if its magnitude is smaller than this parameter.
   */
  private static final double EPS = 1.0e-12;
  /**
   * The interpolator name.
   */
  public static final String NAME = "StepUpper";
  /**
   * The interpolator instance.
   */
  public static final CurveInterpolator INSTANCE = new StepUpperCurveInterpolator();

  /**
   * Restricted constructor.
   */
  private StepUpperCurveInterpolator() {
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

  //-------------------------------------------------------------------------
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
    private final int maxIndex;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      super(xValues, yValues);
      this.xValues = xValues.toArrayUnsafe();
      this.yValues = yValues.toArrayUnsafe();
      this.maxIndex = xValues.size() - 1;
    }

    Bound(Bound base, BoundCurveExtrapolator extrapolatorLeft, BoundCurveExtrapolator extrapolatorRight) {
      super(base, extrapolatorLeft, extrapolatorRight);
      this.xValues = base.xValues;
      this.yValues = base.yValues;
      this.maxIndex = base.maxIndex;
    }

//-------------------------------------------------------------------------
    @Override
    protected double doInterpolate(double xValue) {
      int upperIndex = getUpperBoundIndex(xValue);
      return yValues[upperIndex];
    }

    @Override
    protected double doFirstDerivative(double xValue) {
      return 0d;
    }

    @Override
    protected DoubleArray doParameterSensitivity(double xValue) {
      double[] result = new double[yValues.length];
      int upperIndex = getUpperBoundIndex(xValue);
      result[upperIndex] = 1d;
      return DoubleArray.ofUnsafe(result);
    }

    @Override
    public BoundCurveInterpolator bind(
        BoundCurveExtrapolator extrapolatorLeft,
        BoundCurveExtrapolator extrapolatorRight) {

      return new Bound(this, extrapolatorLeft, extrapolatorRight);
    }

    private int getUpperBoundIndex(double xValue) {
      if (xValue <= xValues[0] + EPS) {
        return 0;
      }
      if (xValue >= xValues[maxIndex - 1] + EPS) {
        return maxIndex;
      }
      int lowerIndex = lowerBoundIndex(xValue, xValues);
      if (Math.abs(xValues[lowerIndex] - xValue) < EPS) {
        return lowerIndex;
      }
      return lowerIndex + 1;
    }
  }

}
