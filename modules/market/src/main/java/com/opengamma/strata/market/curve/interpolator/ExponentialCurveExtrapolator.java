/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation based on a exponential function.
 * <p>
 * Outside the data range the function is
 * an exponential exp(m*x) where m is such that:
 *  - on the left: exp(m * firstXValue) = firstYValue
 *  - on the right: exp(m * lastXValue) = lastYValue
 */
final class ExponentialCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "Exponential";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new ExponentialCurveExtrapolator();

  /**
   * Restricted constructor.
   */
  private ExponentialCurveExtrapolator() {
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
    private final double leftGradient;
    private final double rightGradient;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.lastXValue = xValues.get(nodeCount - 1);
      this.lastYValue = yValues.get(nodeCount - 1);
      // left
      this.leftGradient = Math.log(firstYValue) / firstXValue;
      // right
      this.rightGradient = Math.log(lastYValue) / lastXValue;
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      return Math.exp(leftGradient * xValue);
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return leftGradient * Math.exp(leftGradient * xValue);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      double ex = Math.exp(leftGradient * xValue);
      double[] result = new double[nodeCount];
      result[0] = ex * xValue / (firstXValue * firstYValue);
      return DoubleArray.ofUnsafe(result);
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return Math.exp(rightGradient * xValue);
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return rightGradient * Math.exp(rightGradient * xValue);
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      double ex = Math.exp(rightGradient * xValue);
      double[] result = new double[nodeCount];
      result[nodeCount - 1] = ex * xValue / (lastXValue * lastYValue);
      return DoubleArray.ofUnsafe(result);
    }
  }

}
