/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import java.util.function.Function;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Utility class for converting OpenGamma mathematical objects into
 * <a href="http://commons.apache.org/math/api-2.1/index.html">Commons</a> objects and vice versa.
 */
public final class CommonsMathWrapper {

  // restricted constructor
  private CommonsMathWrapper() {
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a function.
   * 
   * @param f  an OG 1-D function mapping doubles onto doubles
   * @return a Commons univariate real function
   */
  public static UnivariateFunction wrapUnivariate(Function<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return f::apply;
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a matrix.
   * 
   * @param x  an OG 2-D matrix of doubles
   * @return a Commons matrix
   */
  public static RealMatrix wrap(DoubleMatrix x) {
    ArgChecker.notNull(x, "x");
    return new Array2DRowRealMatrix(x.toArrayUnsafe());
  }

  /**
   * Wraps a matrix.
   * 
   * @param x  an OG 1-D vector of doubles
   * @return a Commons matrix 
   */
  public static RealMatrix wrapAsMatrix(DoubleArray x) {
    ArgChecker.notNull(x, "x");
    int n = x.size();
    double[][] y = new double[n][1];
    for (int i = 0; i < n; i++) {
      y[i][0] = x.get(i);
    }
    return new Array2DRowRealMatrix(x.toArrayUnsafe());  // cloned in Array2DRowRealMatrix constructor
  }

  /**
   * Unwraps a matrix.
   * 
   * @param x  a Commons matrix
   * @return an OG 2-D matrix of doubles
   */
  public static DoubleMatrix unwrap(RealMatrix x) {
    ArgChecker.notNull(x, "x");
    return DoubleMatrix.ofUnsafe(x.getData());
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a vector.
   * 
   * @param x  an OG vector of doubles
   * @return a Commons vector
   */
  public static RealVector wrap(DoubleArray x) {
    ArgChecker.notNull(x, "x");
    return new ArrayRealVector(x.toArrayUnsafe()); // cloned in ArrayRealVector constructor
  }

  /**
   * Unwraps a vector.
   * 
   * @param x  a Commons vector
   * @return an OG 1-D matrix of doubles
   */
  public static DoubleArray unwrap(RealVector x) {
    ArgChecker.notNull(x, "x");
    return DoubleArray.ofUnsafe(x.toArray());
  }

}
