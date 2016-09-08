/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Abstract interpolator implementation.
 */
public abstract class AbstractBoundCurveInterpolator
    implements BoundCurveInterpolator {

  /**
   * Negative zero.
   */
  private static long NEGATIVE_ZERO_BITS = Double.doubleToRawLongBits(-0d);

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
  protected AbstractBoundCurveInterpolator(DoubleArray xValues, DoubleArray yValues) {
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
  protected AbstractBoundCurveInterpolator(
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
  public final double interpolate(double xValue) {
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
  protected abstract double doInterpolate(double xValue);

  @Override
  public final double firstDerivative(double xValue) {
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
  protected abstract double doFirstDerivative(double xValue);

  @Override
  public final DoubleArray parameterSensitivity(double xValue) {
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
  protected abstract DoubleArray doParameterSensitivity(double xValue);

  //-------------------------------------------------------------------------
  /**
   * Returns the index of the last value in the input array which is lower than the specified value.
   * <p>
   * The following conditions must be true for this method to work correctly:
   * <ul>
   *   <li>{@code xValues} is sorted in ascending order</li>
   *   <li>{@code xValue} is greater or equal to the first element of {@code xValues}</li>
   *   <li>{@code xValue} is less than or equal to the last element of {@code xValues}</li>
   * </ul>
   * The returned value satisfies:
   * <pre>
   *   0 <= value < xValues.length
   * </pre>
   * <p>
   * The x-values must not be NaN.
   *
   * @param xValue  a value which is less than the last element in {@code xValues}
   * @param xValues  an array of values sorted in ascending order
   * @return the index of the last value in {@code xValues} which is lower than {@code xValue}
   */
  protected static int lowerBoundIndex(double xValue, double[] xValues) {
    // handle -zero, ensure same result as +zero
    if (Double.doubleToRawLongBits(xValue) == NEGATIVE_ZERO_BITS) {
      return lowerBoundIndex(0d, xValues);
    }
    // manual inline of binary search to avoid NaN checks and negation
    int lo = 1;
    int hi = xValues.length - 1;
    while (lo <= hi) {
      // find the middle
      int mid = (lo + hi) >>> 1;
      double midVal = xValues[mid];
      // decide where to search next
      if (midVal < xValue) {
        lo = mid + 1;  // search top half
      } else if (midVal > xValue) {
        hi = mid - 1;  // search bottom half
      } else {
        return mid;  // found
      }
    }
    return lo - 1;
  }

}
