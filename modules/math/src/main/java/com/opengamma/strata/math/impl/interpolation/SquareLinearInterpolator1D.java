/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.InterpolationBoundedValues;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * The interpolation is linear on y^2. The interpolator is used for interpolation on variance for options.
 * All values of y must be positive. 
 */
public class SquareLinearInterpolator1D extends Interpolator1D {

  /* Level below which the value is consider to be 0. */
  private static final double EPS = 1.0E-10;

  @Override
  public double interpolate(final Interpolator1DDataBundle data, final double value) {
    ArgChecker.notNull(value, "Value to be interpolated must not be null");
    ArgChecker.notNull(data, "Data bundle must not be null");
    InterpolationBoundedValues boundedValues = data.getBoundedValues(value);
    double x1 = boundedValues.getLowerBoundKey();
    double y1 = boundedValues.getLowerBoundValue();
    if (boundedValues.getLowerBoundIndex() == data.size() - 1) {
      return y1;
    }
    double x2 = boundedValues.getHigherBoundKey();
    double y2 = boundedValues.getHigherBoundValue();
    double w = (x2 - value) / (x2 - x1);
    double y21 = y1 * y1;
    double y22 = y2 * y2;
    double ySq = w * y21 + (1.0 - w) * y22;
    return Math.sqrt(ySq);
  }

  @Override
  public double firstDerivative(final Interpolator1DDataBundle data, final double value) {
    ArgChecker.notNull(data, "Data bundle must not be null");
    int lowerIndex = data.getLowerBoundIndex(value);
    int index;
    if (lowerIndex == data.size() - 1) {
      index = data.size() - 2;
    } else {
      index = lowerIndex;
    }
    double x1 = data.getKeys()[index];
    double y1 = data.getValues()[index];
    double x2 = data.getKeys()[index + 1];
    double y2 = data.getValues()[index + 1];
    if ((y1 < EPS) && (y2 >= EPS) && (value - x1) < EPS) { // On one vertex with value 0, other vertex not 0
      throw new IllegalArgumentException("ask for first derivative on a value without derivative; value " + value +
          " is close to vertex " + x1 + " and value at vertex is " + y1);
    }
    if ((y2 < EPS) && (y1 >= EPS) && (x2 - value) < EPS) { // On one vertex with value 0, other vertex not 0
      throw new IllegalArgumentException("ask for first derivative on a value without derivative; value " + value +
          " is close to vertex " + x2 + " and value at vertex is " + y2);
    }
    if ((y1 < EPS) && (y2 < EPS)) { // Both vertices have 0 value, return 0.
      return 0.0;
    }
    double w = (x2 - value) / (x2 - x1);
    double y21 = y1 * y1;
    double y22 = y2 * y2;
    double ySq = w * y21 + (1.0 - w) * y22;
    return 0.5 * (y22 - y21) / (x2 - x1) / Math.sqrt(ySq);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(final Interpolator1DDataBundle data, final double value) {
    ArgChecker.notNull(data, "Data bundle must not be null");
    int n = data.size();
    double[] resultSensitivity = new double[n];
    InterpolationBoundedValues boundedValues = data.getBoundedValues(value);
    double x1 = boundedValues.getLowerBoundKey();
    double y1 = boundedValues.getLowerBoundValue();
    int index = boundedValues.getLowerBoundIndex();
    if (index == n - 1) {
      resultSensitivity[n - 1] = 1.0;
      return resultSensitivity;
    }
    double x2 = boundedValues.getHigherBoundKey();
    double y2 = boundedValues.getHigherBoundValue();
    if ((value - x1) < EPS) { // On or very close to Vertex 1
      resultSensitivity[index] = 1.0d;
      return resultSensitivity;
    }
    if ((x2 - value) < EPS) { // On or very close to Vertex 2
      resultSensitivity[index + 1] = 1.0d;
      return resultSensitivity;
    }
    double w2 = (x2 - value) / (x2 - x1);
    if ((y2 < EPS) && (y1 < EPS)) { // Both values very close to 0
      resultSensitivity[index] = Math.sqrt(w2);
      resultSensitivity[index + 1] = Math.sqrt(1.0d - w2);
      return resultSensitivity;
    }
    double y21 = y1 * y1;
    double y22 = y2 * y2;
    double ySq = w2 * y21 + (1.0 - w2) * y22;
    // Backward
    double ySqBar = 0.5 / Math.sqrt(ySq);
    double y22Bar = (1.0 - w2) * ySqBar;
    double y21Bar = w2 * ySqBar;
    double y1Bar = 2 * y1 * y21Bar;
    double y2Bar = 2 * y2 * y22Bar;
    resultSensitivity[index] = y1Bar;
    resultSensitivity[index + 1] = y2Bar;
    return resultSensitivity;
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    ArgChecker.notNull(y, "y");
    int nY = y.length;
    for (int i = 0; i < nY; ++i) {
      ArgChecker.isTrue(y[i] >= 0.0, "All values in y must be positive");
    }
    return new ArrayInterpolator1DDataBundle(x, y);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    ArgChecker.notNull(y, "y");
    int nY = y.length;
    for (int i = 0; i < nY; ++i) {
      ArgChecker.isTrue(y[i] >= 0.0, "All values in y must be positive");
    }
    return new ArrayInterpolator1DDataBundle(x, y, true);
  }

}
