/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that returns the y-value of the first or last node.
 * <p>
 * When left extrapolating, the y-value of the first node is returned.
 * When right extrapolating, the y-value of the last node is returned.
 */
final class FlatCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "Flat";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new FlatCurveExtrapolator();

  /**
   * Restricted constructor.
   */
  private FlatCurveExtrapolator() {
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
    return new Bound(xValues, yValues);
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
    private final double firstYValue;
    private final double lastYValue;
    private final DoubleArray leftSensitivity;
    private final DoubleArray rightSensitivity;

    Bound(DoubleArray xValues, DoubleArray yValues) {
      this.nodeCount = xValues.size();
      this.firstYValue = yValues.get(0);
      this.lastYValue = yValues.get(nodeCount - 1);
      double[] left = new double[nodeCount];
      left[0] = 1d;
      this.leftSensitivity = DoubleArray.ofUnsafe(left);
      double[] right = new double[nodeCount];
      right[nodeCount - 1] = 1d;
      this.rightSensitivity = DoubleArray.ofUnsafe(right);
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      return firstYValue;
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return 0d;
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      return leftSensitivity;
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return lastYValue;
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return 0d;
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      return rightSensitivity;
    }
  }

}
