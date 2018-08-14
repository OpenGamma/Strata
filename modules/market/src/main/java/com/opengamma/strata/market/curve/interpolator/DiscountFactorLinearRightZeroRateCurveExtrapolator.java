/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that is designed for extrapolating a zero rate curve for the far end.
 * <p>
 * The linear interpolation is applied discount factor values converted from the input zero rates. 
 * The gradient of the linear function is determined so that the first derivative of the discount 
 * factor is continuous at the last node.
 */
class DiscountFactorLinearRightZeroRateCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "DiscountFactorLinearRightZeroRateCurve";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new DiscountFactorLinearRightZeroRateCurveExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private DiscountFactorLinearRightZeroRateCurveExtrapolator() {
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
    private final double lastXValue;
    private final double lastYValue;
    private final double lastDf;
    private final double eps;
    private final double rightYGradient;
    private final DoubleArray rightYSens;

    private final double coef1;
    private final double coef0;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.lastXValue = xValues.get(nodeCount - 1);
      this.lastYValue = yValues.get(nodeCount - 1);
      this.lastDf = Math.exp(-lastXValue * lastYValue);
      this.eps = EPS * (lastXValue - xValues.get(0));
      this.rightYGradient = (lastYValue - interpolator.interpolate(lastXValue - eps)) / eps;
      this.rightYSens = interpolator.parameterSensitivity(lastXValue - eps).multipliedBy(-1d);
      this.coef1 = -lastYValue * lastDf - lastXValue * lastDf * rightYGradient;
      this.coef0 = lastDf - coef1 * lastXValue;
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      throw new IllegalArgumentException(
          "DiscountFactorLinearRightZeroRateCurveExtrapolator cannot be used for left extrapolation");
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      throw new IllegalArgumentException(
          "DiscountFactorLinearRightZeroRateCurveExtrapolator cannot be used for left extrapolation");
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      throw new IllegalArgumentException(
          "DiscountFactorLinearRightZeroRateCurveExtrapolator cannot be used for left extrapolation");
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      if (lastXValue <= 0d) {
        throw new IllegalArgumentException("X value of the right endpoint must be positive");
      }
      return -Math.log(coef1 * xValue + coef0) / xValue;
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      if (lastXValue <= 0d) {
        throw new IllegalArgumentException("X value of the right endpoint must be positive");
      }
      double df = coef1 * xValue + coef0;
      double value = -Math.log(df) / xValue;
      return -(value + coef1 / df) / xValue;
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      if (lastXValue <= 0d) {
        throw new IllegalArgumentException("X value of the right endpoint must be positive");
      }
      double df = coef1 * xValue + coef0;
      double[] result = rightYSens.toArray();
      double factor = xValue - lastXValue;
      int minusOne = nodeCount - 1;
      for (int i = 0; i < minusOne; i++) {
        result[i] *= factor / eps;
      }
      result[minusOne] = (1d + result[minusOne]) * factor / eps;
      result[minusOne] +=
          (1d / lastXValue - lastYValue - lastXValue * rightYGradient) * xValue +
              lastXValue * lastYValue + lastXValue * lastXValue * rightYGradient;
      return DoubleArray.ofUnsafe(result).multipliedBy(lastXValue * lastDf / (xValue * df));
    }
  }

}
