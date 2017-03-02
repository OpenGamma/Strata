/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation.
 * <p>
 * The extrapolant is {@code exp(f(x))} where {@code f(x)} is a linear function
 * which is smoothly connected with a log-interpolator {@code exp(F(x))}.
 */
final class LogLinearCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "LogLinear";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new LogLinearCurveExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private LogLinearCurveExtrapolator() {
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
    private final double firstYValueLog;
    private final double lastXValue;
    private final double lastYValue;
    private final double lastYValueLog;
    private final double eps;
    private final double leftGradient;
    private final double leftResValueInterpolator;
    private final DoubleArray leftSens;
    private final double rightGradient;
    private final double rightResValueInterpolator;
    private final DoubleArray rightSens;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.firstYValueLog = Math.log(firstYValue);
      this.lastXValue = xValues.get(nodeCount - 1);
      this.lastYValue = yValues.get(nodeCount - 1);
      this.lastYValueLog = Math.log(lastYValue);
      this.eps = EPS * (lastXValue - firstXValue);
      // left
      this.leftGradient = interpolator.firstDerivative(firstXValue) / interpolator.interpolate(firstXValue);
      this.leftResValueInterpolator = interpolator.interpolate(firstXValue + eps);
      this.leftSens = interpolator.parameterSensitivity(firstXValue + eps);
      // right
      this.rightGradient = interpolator.firstDerivative(lastXValue) / interpolator.interpolate(lastXValue);
      this.rightResValueInterpolator = interpolator.interpolate(lastXValue - eps);
      this.rightSens = interpolator.parameterSensitivity(lastXValue - eps);
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      return Math.exp(firstYValueLog + (xValue - firstXValue) * leftGradient);
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return leftGradient * Math.exp(firstYValueLog + (xValue - firstXValue) * leftGradient);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      double[] result = leftSens.toArray();
      double resValueExtrapolator = leftExtrapolate(xValue);
      double factor1 = (xValue - firstXValue) / eps;
      double factor2 = factor1 * resValueExtrapolator / leftResValueInterpolator;
      int n = result.length;
      for (int i = 1; i < n; i++) {
        result[i] *= factor2;
      }
      result[0] = result[0] * factor2 + (1d - factor1) * resValueExtrapolator / firstYValue;
      return DoubleArray.ofUnsafe(result);
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return Math.exp(lastYValueLog + (xValue - lastXValue) * rightGradient);
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return rightGradient * Math.exp(lastYValueLog + (xValue - lastXValue) * rightGradient);
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      double[] result = rightSens.toArray();
      double resValueExtrapolator = rightExtrapolate(xValue);
      double factor1 = (xValue - lastXValue) / eps;
      double factor2 = factor1 * resValueExtrapolator / rightResValueInterpolator;
      int n = result.length;
      for (int i = 0; i < n - 1; i++) {
        result[i] *= -factor2;
      }
      result[n - 1] = (1d + factor1) * resValueExtrapolator / lastYValue - result[n - 1] * factor2;
      return DoubleArray.ofUnsafe(result);
    }
  }

}
