/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that is designed for extrapolating a discount factor where the
 * trivial point (0,1) is NOT involved in the data.
 * The extrapolation is completed by applying a quadratic extrapolant on the discount
 * factor (not log of the discount factor), where the point (0,1) is inserted and
 * the first derivative value is assumed to be continuous at the first x-value.
 */
final class QuadraticLeftCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "QuadraticLeft";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new QuadraticLeftCurveExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private QuadraticLeftCurveExtrapolator() {
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
    private final double eps;
    private final double leftQuadCoef;
    private final double leftLinCoef;
    private final DoubleArray leftSens;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.lastXValue = xValues.get(nodeCount - 1);
      double gradient = interpolator.firstDerivative(firstXValue);
      this.eps = EPS * (lastXValue - firstXValue);
      this.leftQuadCoef = gradient / firstXValue - (firstYValue - 1d) / firstXValue / firstXValue;
      this.leftLinCoef = -gradient + 2d * (firstYValue - 1d) / firstXValue;
      this.leftSens = interpolator.parameterSensitivity(firstXValue + eps);
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      if (firstXValue == 0d) {
        throw new IllegalArgumentException("The trivial point at x = 0 is already included");
      }
      return leftQuadCoef * xValue * xValue + leftLinCoef * xValue + 1d;
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      if (firstXValue == 0d) {
        throw new IllegalArgumentException("The trivial point at x = 0 is already included");
      }
      return 2d * leftQuadCoef * xValue + leftLinCoef;
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      if (firstXValue == 0d) {
        throw new IllegalArgumentException("The trivial point at x = 0 is already included");
      }
      double[] result = leftSens.toArray();
      for (int i = 1; i < nodeCount; i++) {
        double tmp = result[i] * xValue / eps;
        result[i] = tmp / firstXValue * xValue - tmp;
      }
      double tmp = (result[0] - 1d) / eps;
      result[0] = (tmp / firstXValue - 1d / firstXValue / firstXValue) * xValue * xValue + (2d / firstXValue - tmp) * xValue;
      return DoubleArray.ofUnsafe(result);
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      throw new IllegalArgumentException("QuadraticLeft extrapolator cannot be used for right extrapolation");
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      throw new IllegalArgumentException("QuadraticLeft extrapolator cannot be used for right extrapolation");
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      throw new IllegalArgumentException("QuadraticLeft extrapolator cannot be used for right extrapolation");
    }
  }

}
