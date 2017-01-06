/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that returns a value linearly in terms of {@code (x[i], x[i] * y[i])} without sensitivity 
 * precomputed. 
 * <p>
 * The gradient of the extrapolation is obtained from the gradient of the interpolated curve on {@code (x[i], x[i] * y[i])}  
 * at the first/last node.
 * <p>
 * The extrapolation is ambiguous at x=0. Thus the following rule applies: 
 * The x value of the first node must be strictly negative for the left extrapolation, whereas the x value of 
 * the last node must be strictly positive for the right extrapolation.
 * <p>
 * Use {@code CurveExtrapolators.PRODUCT_LINEAR} for parameter sensitivity. 
 */
public class ProductLinearSimpleCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "ProductLinearSimple";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new ProductLinearSimpleCurveExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private ProductLinearSimpleCurveExtrapolator() {
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
    private final double lastGradient;
    private final double firstXValue;
    private final double firstYValue;
    private final double firstGradient;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.lastXValue = xValues.get(nodeCount - 1);
      this.lastYValue = yValues.get(nodeCount - 1);
      // left
      this.firstGradient = interpolator.firstDerivative(firstXValue);
      // right
      this.lastGradient = interpolator.firstDerivative(lastXValue);
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      ArgChecker.isTrue(firstXValue < -EPS, "the first x value must be negative for left extrapolation");
      return firstGradient * firstXValue * (1d - firstXValue / xValue) + firstYValue;
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      ArgChecker.isTrue(firstXValue < -EPS, "the first x value must be negative for left extrapolation");
      return firstGradient * Math.pow(firstXValue / xValue, 2);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      throw new UnsupportedOperationException("Use ProductLinearCurveExtrapolator for sensitivity");
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      ArgChecker.isTrue(lastXValue > EPS, "the last x value must be positive for right extrapolation");
      return lastGradient * lastXValue * (1d - lastXValue / xValue) + lastYValue;
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      ArgChecker.isTrue(lastXValue > EPS, "the last x value must be positive for right extrapolation");
      return lastGradient * Math.pow(lastXValue / xValue, 2);
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      throw new UnsupportedOperationException("Use ProductLinearCurveExtrapolator for sensitivity");
    }
  }

}
