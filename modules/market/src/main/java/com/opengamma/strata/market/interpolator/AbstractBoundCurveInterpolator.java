/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Abstract interpolator implementation.
 */
abstract class AbstractBoundCurveInterpolator
    implements BoundCurveInterpolator {

  /**
   * The left extrapolator.
   */
  private final BoundCurveExtrapolator extrapolatorLeft;
  /**
   * The right extrapolator.
   */
  private final BoundCurveExtrapolator extrapolatorRight;
  /**
   * The x-value of the first node.
   */
  private final double firstXValue;
  /**
   * The x-value of the last node.
   */
  private final double lastXValue;
  /**
   * The y-value of the last node.
   */
  private final double lastYValue;

  /**
   * Creates an instance.
   * 
   * @param xValues  the x-values of the curve, must be sorted from low to high
   * @param yValues  the y-values of the curve
   */
  AbstractBoundCurveInterpolator(DoubleArray xValues, DoubleArray yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    int size = xValues.size();
    ArgChecker.isTrue(size == yValues.size(), "Curve node arrays must have same size");
    ArgChecker.isTrue(size > 1, "Curve node arrays must have at least two nodes");
    this.extrapolatorLeft = ExceptionCurveExtrapolator.INSTANCE;
    this.extrapolatorRight = ExceptionCurveExtrapolator.INSTANCE;
    this.firstXValue = xValues.get(0);
    this.lastXValue = xValues.get(size - 1);
    this.lastYValue = yValues.get(size - 1);
  }

  /**
   * Creates an instance.
   * 
   * @param base  the base interpolator
   * @param extrapolatorLeft  the extrapolator for x-values on the left
   * @param extrapolatorRight  the extrapolator for x-values on the right
   */
  AbstractBoundCurveInterpolator(
      AbstractBoundCurveInterpolator base,
      BoundCurveExtrapolator extrapolatorLeft,
      BoundCurveExtrapolator extrapolatorRight) {

    this.extrapolatorLeft = ArgChecker.notNull(extrapolatorLeft, "extrapolatorLeft");
    this.extrapolatorRight = ArgChecker.notNull(extrapolatorRight, "extrapolatorRight");
    this.firstXValue = base.firstXValue;
    this.lastXValue = base.lastXValue;
    this.lastYValue = base.lastYValue;
  }

  //-------------------------------------------------------------------------
  @Override
  public double interpolate(double xValue) {
    if (xValue < firstXValue) {
      return extrapolatorLeft.leftExtrapolate(xValue);
    } else if (xValue > lastXValue) {
      return extrapolatorRight.rightExtrapolate(xValue);
    } else if (xValue == lastXValue) {
      return lastYValue;
    }
    return doInterpolate(xValue);
  }

  /**
   * Method for subclasses to calculate the interpolated value.
   * 
   * @param xValue  the x-value
   * @return the interpolated y-value
   */
  abstract double doInterpolate(double xValue);

  @Override
  public double firstDerivative(double xValue) {
    if (xValue < firstXValue) {
      return extrapolatorLeft.leftExtrapolateFirstDerivative(xValue);
    } else if (xValue > lastXValue) {
      return extrapolatorRight.rightExtrapolateFirstDerivative(xValue);
    }
    return doFirstDerivative(xValue);
  }

  /**
   * Method for subclasses to calculate the first derivative.
   * 
   * @param xValue  the x-value
   * @return the first derivative
   */
  abstract double doFirstDerivative(double xValue);

  @Override
  public DoubleArray parameterSensitivity(double xValue) {
    if (xValue < firstXValue) {
      return extrapolatorLeft.leftExtrapolateParameterSensitivity(xValue);
    } else if (xValue > lastXValue) {
      return extrapolatorRight.rightExtrapolateParameterSensitivity(xValue);
    }
    return doParameterSensitivity(xValue);
  }

  /**
   * Method for subclasses to calculate parameter sensitivity.
   * 
   * @param xValue  the x-value
   * @return the parameter sensitivity
   */
  abstract DoubleArray doParameterSensitivity(double xValue);

  //-------------------------------------------------------------------------
  static int lowerBoundIndex(double xValue, double[] xValues) {
    int index = Arrays.binarySearch(xValues, xValue);
    // break out if find an exact match
    if (index >= 0) {
      return index;
    }
    index = -index - 2;
    // handle -zero, ensure same result as +zero
    if (xValue == -0d && index < xValues.length - 1 && xValues[index + 1] == 0d) {
      index++;
    }
    return index;
  }

}
