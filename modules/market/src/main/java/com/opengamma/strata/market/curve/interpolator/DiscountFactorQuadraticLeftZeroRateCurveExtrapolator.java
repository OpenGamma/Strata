/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that is designed for extrapolating a zero rate curve for the near end.
 * <p>
 * The extrapolation is completed by applying a quadratic extrapolant on the discount
 * factor, where the point (0,1) is inserted and
 * the first derivative value is assumed to be continuous at the first x-value.
 */
class DiscountFactorQuadraticLeftZeroRateCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "DiscountFactorQuadraticLeftZeroRate";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new DiscountFactorQuadraticLeftZeroRateCurveExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private DiscountFactorQuadraticLeftZeroRateCurveExtrapolator() {
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
    private final double firstYGradient;
    private final double firstDfValue;
    private final double eps;
    private final double leftQuadCoef;
    private final double leftLinCoef;
    private final Supplier<DoubleArray> leftSens;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      this.nodeCount = xValues.size();
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.firstDfValue = Math.exp(-firstXValue * firstYValue);
      this.firstYGradient = interpolator.firstDerivative(firstXValue);
      double gradient = -firstYValue * firstDfValue - firstXValue * firstDfValue * firstYGradient;
      this.eps = EPS * (xValues.get(nodeCount - 1) - firstXValue);
      this.leftQuadCoef = gradient / firstXValue - (firstDfValue - 1d) / firstXValue / firstXValue;
      this.leftLinCoef = -gradient + 2d * (firstDfValue - 1d) / firstXValue;
      this.leftSens = Suppliers.memoize(() -> interpolator.parameterSensitivity(firstXValue + eps));
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      if (firstXValue == 0d) {
        throw new IllegalArgumentException("The trivial point at x = 0 is already included");
      }
      if (Math.abs(xValue) < eps) {
        return -leftLinCoef - (leftQuadCoef - 0.5d * leftLinCoef * leftLinCoef) * xValue -
            xValue * xValue *
                (leftLinCoef * leftLinCoef * leftLinCoef / 3d - leftQuadCoef * leftLinCoef);
      }
      double df = leftQuadCoef * xValue * xValue + leftLinCoef * xValue + 1d;
      return -Math.log(df) / xValue;
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      if (firstXValue == 0d) {
        throw new IllegalArgumentException("The trivial point at x = 0 is already included");
      }
      if (Math.abs(xValue) < eps) {
        return -leftQuadCoef + 0.5d * leftLinCoef * leftLinCoef - xValue *
            (2d * leftLinCoef * leftLinCoef * leftLinCoef / 3d - 2d * leftQuadCoef * leftLinCoef);
      }
      double gradDf = 2d * leftQuadCoef * xValue + leftLinCoef;
      double df = leftQuadCoef * xValue * xValue + leftLinCoef * xValue + 1d;
      return Math.log(df) / Math.pow(xValue, 2) - gradDf / (df * xValue);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      if (firstXValue == 0d) {
        throw new IllegalArgumentException("The trivial point at x = 0 is already included");
      }
      double[] sensiZero = leftSens.get().toArray();
      double xQuad = xValue * xValue;
      if (Math.abs(xValue) < eps) {
        double coef1 = -(leftLinCoef * leftLinCoef - leftQuadCoef) * xQuad +
            leftLinCoef * xValue - 1d;
        double coef2 = -xValue + leftLinCoef * xQuad;
        double factor = firstDfValue * (firstXValue * coef1 - coef2);
        for (int i = 1; i < nodeCount; i++) {
          sensiZero[i] *= factor / eps;
        }
        sensiZero[0] = (sensiZero[0] - 1d) * factor / eps;
        sensiZero[0] += -firstDfValue * coef1 *
            (1d + firstXValue * firstYValue + firstXValue * firstXValue * firstYGradient) +
            firstDfValue * (firstYValue + firstXValue * firstYGradient) * coef2;
        return DoubleArray.ofUnsafe(sensiZero);
      }
      double df = leftQuadCoef * xQuad + leftLinCoef * xValue + 1d;
      double factor = xQuad / firstXValue - xValue;
      for (int i = 1; i < nodeCount; i++) {
        sensiZero[i] *= factor / eps;
      }
      sensiZero[0] = (sensiZero[0] - 1d) * factor / eps;
      sensiZero[0] += xValue / firstXValue -
          (xQuad / firstXValue - xValue) * (firstYValue + firstXValue * firstYGradient);
      return DoubleArray.ofUnsafe(sensiZero)
          .multipliedBy(firstXValue * firstDfValue / (xValue * df));
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      throw new IllegalArgumentException(
          "QuadraticLeftZeroRateCurveExtrapolator cannot be used for right extrapolation");
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      throw new IllegalArgumentException(
          "QuadraticLeftZeroRateCurveExtrapolator cannot be used for right extrapolation");
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      throw new IllegalArgumentException(
          "QuadraticLeftZeroRateCurveExtrapolator cannot be used for right extrapolation");
    }
  }

}
