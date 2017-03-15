/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that returns a value linearly in terms of {@code (x[i], x[i] * y[i])}. 
 * <p>
 * The gradient of the extrapolation is obtained from the gradient of the interpolated curve on {@code (x[i], x[i] * y[i])}  
 * at the first/last node.
 * <p>
 * The extrapolation is ambiguous at x=0. Thus the following rule applies: 
 * The x value of the first node must be strictly negative for the left extrapolation, whereas the x value of 
 * the last node must be strictly positive for the right extrapolation.
 */
final class ProductLinearCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "ProductLinear";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new ProductLinearCurveExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private ProductLinearCurveExtrapolator() {
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
    private final double eps;
    private final double lastGradient;
    private final Supplier<DoubleArray> lastSens;
    private final Supplier<DoubleArray> lastGradSens;

    private final double firstXValue;
    private final double firstYValue;
    private final double firstGradient;
    private final Supplier<DoubleArray> firstSens;
    private final Supplier<DoubleArray> firstGradSens;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.lastXValue = xValues.get(nodeCount - 1);
      this.lastYValue = yValues.get(nodeCount - 1);
      this.eps = EPS * (lastXValue - firstXValue);
      // left
      this.firstGradient = interpolator.firstDerivative(firstXValue);
      this.firstSens = Suppliers.memoize(() -> interpolator.parameterSensitivity(firstXValue));
      this.firstGradSens =
          Suppliers.memoize(() -> interpolator.parameterSensitivity(firstXValue + eps).minus(firstSens.get()).dividedBy(eps));
      // right
      this.lastGradient = interpolator.firstDerivative(lastXValue);
      this.lastSens = Suppliers.memoize(() -> interpolator.parameterSensitivity(lastXValue));
      this.lastGradSens =
          Suppliers.memoize(() -> lastSens.get().minus(interpolator.parameterSensitivity(lastXValue - eps)).dividedBy(eps));
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
      ArgChecker.isTrue(firstXValue < -EPS, "the first x value must be negative for left extrapolation");
      double factor = (1d - firstXValue / xValue) * firstXValue;
      return firstGradSens.get().multipliedBy(factor).plus(firstSens.get());
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
      ArgChecker.isTrue(lastXValue > EPS, "the last x value must be positive for right extrapolation");
      double factor = (1d - lastXValue / xValue) * lastXValue;
      return lastGradSens.get().multipliedBy(factor).plus(lastSens.get());
    }
  }

}
