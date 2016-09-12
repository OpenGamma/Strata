/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that returns a value linearly from the gradient at the first or last node.
 */
final class LinearCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "Linear";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new LinearCurveExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private LinearCurveExtrapolator() {
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
  public BoundCurveExtrapolator bind(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
    return new Bound(xValues, yValues, interpolator);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Bound extrapolator.
   */
  static class Bound implements BoundCurveExtrapolator {
    private final int nodeCount;
    private final double firstXValue;
    private final double firstYValue;
    private final double lastXValue;
    private final double lastYValue;
    private final double eps;
    private final double leftGradient;
    private final DoubleArray leftSens;
    private final double rightGradient;
    private final DoubleArray rightSens;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.lastXValue = xValues.get(nodeCount - 1);
      this.lastYValue = yValues.get(nodeCount - 1);
      this.eps = EPS * (lastXValue - firstXValue);
      // left
      this.leftGradient = (interpolator.interpolate(firstXValue + eps) - firstYValue) / eps;
      this.leftSens = interpolator.parameterSensitivity(firstXValue + eps);
      // right
      this.rightGradient = (lastYValue - interpolator.interpolate(lastXValue - eps)) / eps;
      this.rightSens = interpolator.parameterSensitivity(lastXValue - eps);
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      return firstYValue + (xValue - firstXValue) * leftGradient;
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return leftGradient;
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      double[] result = leftSens.toArray();
      int n = result.length;
      for (int i = 1; i < n; i++) {
        result[i] = result[i] * (xValue - firstXValue) / eps;
      }
      result[0] = 1 + (result[0] - 1) * (xValue - firstXValue) / eps;
      return DoubleArray.ofUnsafe(result);
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return lastYValue + (xValue - lastXValue) * rightGradient;
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return rightGradient;
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      double[] result = rightSens.toArray();
      int n = result.length;
      for (int i = 0; i < n - 1; i++) {
        result[i] = -result[i] * (xValue - lastXValue) / eps;
      }
      result[n - 1] = 1 + (1 - result[n - 1]) * (xValue - lastXValue) / eps;
      return DoubleArray.ofUnsafe(result);
    }
  }

}
