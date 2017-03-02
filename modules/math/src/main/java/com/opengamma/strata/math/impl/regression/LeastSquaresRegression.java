/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public abstract class LeastSquaresRegression {

  public abstract LeastSquaresRegressionResult regress(double[][] x, double[][] weights, double[] y, boolean useIntercept);

  protected void checkData(double[][] x, double[][] weights, double[] y) {
    checkData(x, y);
    if (weights != null) {
      if (weights.length == 0) {
        throw new IllegalArgumentException("No data in weights array");
      }
      if (weights.length != x.length) {
        throw new IllegalArgumentException("Independent variable and weight arrays are not the same length");
      }
      int n = weights[0].length;
      for (double[] w : weights) {
        if (w.length != n) {
          throw new IllegalArgumentException("Need a rectangular array of weight");
        }
      }
    }
  }

  protected void checkData(double[][] x, double[] weights, double[] y) {
    checkData(x, y);
    if (weights != null) {
      if (weights.length == 0) {
        throw new IllegalArgumentException("No data in weights array");
      }
      if (weights.length != x.length) {
        throw new IllegalArgumentException("Independent variable and weight arrays are not the same length");
      }
    }
  }

  protected void checkData(double[][] x, double[] y) {
    if (x == null) {
      throw new IllegalArgumentException("Independent variable array was null");
    }
    if (y == null) {
      throw new IllegalArgumentException("Dependent variable array was null");
    }
    if (x.length == 0) {
      throw new IllegalArgumentException("No data in independent variable array");
    }
    if (y.length == 0) {
      throw new IllegalArgumentException("No data in dependent variable array");
    }
    if (x.length != y.length) {
      throw new IllegalArgumentException(
          "Dependent and independent variable arrays are not the same length: have " + x.length + " and " + y.length);
    }
    int n = x[0].length;
    for (double[] x1 : x) {
      if (x1.length != n) {
        throw new IllegalArgumentException("Need a rectangular array of independent variables");
      }
    }
    if (y.length <= x[0].length) {
      throw new IllegalArgumentException(
          "Insufficient data; there are " + y.length + " variables but only " + x[0].length + " data points");
    }
  }

  protected double[][] addInterceptVariable(double[][] x, boolean useIntercept) {
    double[][] result = useIntercept ? new double[x.length][x[0].length + 1] : new double[x.length][x[0].length];
    for (int i = 0; i < x.length; i++) {
      if (useIntercept) {
        result[i][0] = 1.;
        for (int j = 1; j < x[0].length + 1; j++) {
          result[i][j] = x[i][j - 1];
        }
      } else {
        for (int j = 0; j < x[0].length; j++) {
          result[i][j] = x[i][j];
        }
      }
    }
    return result;
  }

  protected double[][] convertArray(double[][] x) {
    double[][] result = new double[x.length][x[0].length];
    for (int i = 0; i < result.length; i++) {
      for (int j = 0; j < result[0].length; j++) {
        result[i][j] = x[i][j];
      }
    }
    return result;
  }

  protected double[] convertArray(double[] x) {
    double[] result = new double[x.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = x[i];
    }
    return result;
  }

  protected double[] writeArrayAsVector(double[][] x) {
    ArgChecker.isTrue(x[0].length == 1, "Trying to convert matrix to vector");
    double[] result = new double[x.length];
    for (int i = 0; i < x.length; i++) {
      result[i] = x[i][0];
    }
    return result;
  }

}
