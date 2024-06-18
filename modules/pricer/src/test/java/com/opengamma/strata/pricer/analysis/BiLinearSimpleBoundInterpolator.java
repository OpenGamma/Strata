/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;

/**
 * Specific implementation of the bi-linear surface interpolation. Designed for fast computation.
 */
public class BiLinearSimpleBoundInterpolator 
    implements BoundSurfaceInterpolator{

  /** Negative zero. */
  private static final long NEGATIVE_ZERO_BITS = Double.doubleToRawLongBits(-0d);
  
  /** the X-values */
  private final double[] xValues;
  /** the Y-values */
  private final double[] yValues;
  /** the Z-values associated to the grid of X and Y values. */
  private final double[][] zValues;
  
  /**
   * Constructor. 
   * 
   * @param xValues  the X-values as a 1-D array
   * @param yValues  the Y-values as a 1-D array
   * @param zValues  the Z-values associated to the grid of X and Y values
   */
  public BiLinearSimpleBoundInterpolator(double[] xValues, double[] yValues, double[][] zValues) {
    ArgChecker.isTrue(zValues.length == xValues.length);
    ArgChecker.isTrue(zValues[0].length == yValues.length);
    this.xValues = xValues;
    this.yValues = yValues;
    this.zValues = zValues;
  }

  @Override
  public double interpolate(double x, double y) {
    int iX = Math.min(lowerBoundIndex(x, xValues), xValues.length - 2);
    int iY = Math.min(lowerBoundIndex(y, yValues), yValues.length - 2);
    double alphaX = (xValues[iX+1] - x) / (xValues[iX+1] - xValues[iX]);
    double alphaY = (yValues[iY+1] - y) / (yValues[iY+1] - yValues[iY]);
    double[] zIy = new double[2];
    for(int k=0; k<2; k++) {
      zIy[k] = alphaX * zValues[iX][iY+k] + (1-alphaX) * zValues[iX+1][iY+k];
    }
    double z = alphaY * zIy[0]+ (1-alphaY) * zIy[1];
    return z;
  }

  @Override
  public ValueDerivatives firstPartialDerivatives(double x, double y) {
    // TODO
    return null;
  }

  @Override
  public DoubleArray parameterSensitivity(double x, double y) {
    // TODO: order? parameters as 2D array
    return null;
  }
  
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
   * @param value  a value which is less than the last element in {@code xValues}
   * @param arrayValues  an array of values sorted in ascending order
   * @return the index of the last value in {@code xValues} which is lower than {@code xValue}
   */
  protected static int lowerBoundIndex(double value, double[] arrayValues) {
    // handle -zero, ensure same result as +zero
    if (Double.doubleToRawLongBits(value) == NEGATIVE_ZERO_BITS) {
      return lowerBoundIndex(0d, arrayValues);
    }
    // manual inline of binary search to avoid NaN checks and negation
    int lo = 1;
    int hi = arrayValues.length - 1;
    while (lo <= hi) {
      // find the middle
      int mid = (lo + hi) >>> 1;
      double midVal = arrayValues[mid];
      // decide where to search next
      if (midVal < value) {
        lo = mid + 1;  // search top half
      } else if (midVal > value) {
        hi = mid - 1;  // search bottom half
      } else {
        return mid;  // found
      }
    }
    return lo - 1;
  }

}
