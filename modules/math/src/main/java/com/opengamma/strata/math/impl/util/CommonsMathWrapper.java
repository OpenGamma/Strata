/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NonMonotonicSequenceException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.PointValuePair;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.ComplexNumber;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.function.FunctionND;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

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
  public static UnivariateFunction wrapUnivariate(Function1D<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return f::evaluate;
  }

  /**
   * Wraps a function.
   * 
   * @param f  an OG 1-D function mapping doubles onto doubles
   * @return a Commons univariate real function
   */
  public static MultivariateFunction wrapMultivariate(Function1D<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return point -> {
      int n = point.length;
      Double[] coordinate = new Double[n];
      for (int i = 0; i < n; i++) {
        coordinate[i] = point[i];
      }
      return f.evaluate(coordinate);
    };
  }

  /**
   * Wraps a function.
   * 
   * @param f  an OG 1-D function mapping vectors of doubles onto doubles
   * @return a Commons multivariate real function
   */
  public static MultivariateFunction wrapMultivariateVector(Function1D<DoubleMatrix1D, Double> f) {
    ArgChecker.notNull(f, "f");
    return point -> f.evaluate(new DoubleMatrix1D(point));
  }

  /**
   * Wraps a function.
   * 
   * @param f  an OG n-D function mapping doubles onto doubles
   * @return a Commons multivariate real function
   */
  public static MultivariateFunction wrap(FunctionND<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return point -> {
      int n = point.length;
      Double[] coordinate = new Double[n];
      for (int i = 0; i < n; i++) {
        coordinate[i] = point[i];
      }
      return f.evaluate(coordinate);
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a matrix.
   * 
   * @param x  an OG 2-D matrix of doubles
   * @return a Commons matrix
   */
  public static RealMatrix wrap(DoubleMatrix2D x) {
    ArgChecker.notNull(x, "x");
    return new Array2DRowRealMatrix(x.getData());
  }

  /**
   * Wraps a matrix.
   * 
   * @param x  an OG 1-D vector of doubles
   * @return a Commons matrix 
   */
  public static RealMatrix wrapAsMatrix(DoubleMatrix1D x) {
    ArgChecker.notNull(x, "x");
    int n = x.getNumberOfElements();
    double[][] y = new double[n][1];
    for (int i = 0; i < n; i++) {
      y[i][0] = x.getEntry(i);
    }
    return new Array2DRowRealMatrix(x.getData());
  }

  /**
   * Unwraps a matrix.
   * 
   * @param x  a Commons matrix
   * @return an OG 2-D matrix of doubles
   */
  public static DoubleMatrix2D unwrap(RealMatrix x) {
    ArgChecker.notNull(x, "x");
    return new DoubleMatrix2D(x.getData());
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a vector.
   * 
   * @param x  an OG vector of doubles
   * @return a Commons vector
   */
  public static RealVector wrap(DoubleMatrix1D x) {
    ArgChecker.notNull(x, "x");
    return new ArrayRealVector(x.getData());
  }

  /**
   * Unwraps a vector.
   * 
   * @param x  a Commons vector
   * @return an OG 1-D matrix of doubles
   */
  public static DoubleMatrix1D unwrap(RealVector x) {
    ArgChecker.notNull(x, "x");
    return new DoubleMatrix1D(x.toArray());
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a complex number.
   * 
   * @param z An OG complex number
   * @return a Commons complex number
   */
  public static Complex wrap(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new Complex(z.getReal(), z.getImaginary());
  }

  //-------------------------------------------------------------------------
  /**
   * Unwraps a Lagrange.
   * 
   * @param lagrange  a Commons polynomial in Lagrange form
   * @return an OG 1-D function mapping doubles to doubles
   */
  public static Function1D<Double, Double> unwrap(PolynomialFunctionLagrangeForm lagrange) {
    ArgChecker.notNull(lagrange, "lagrange");
    return new Function1D<Double, Double>() {

      @Override
      public Double evaluate(Double x) {
        try {
          return lagrange.value(x);
        } catch (DimensionMismatchException | NonMonotonicSequenceException | NumberIsTooSmallException e) {
          throw new MathException(e);
        }
      }

    };
  }

  /**
   * Unwraps a pair.
   * 
   * @param x  a Commons pair of <i>(x, f(x))</i>
   * @return a matrix of double with the <i>x</i> as the first element and <i>f(x)</i> the second
   */
  public static double[] unwrap(PointValuePair x) {
    ArgChecker.notNull(x, "x");
    return x.getPoint();
  }

  /**
   * Wraps a differentiable univariate real function.
   * 
   * @param f  an OG 1-D function mapping doubles to doubles
   * @return a Commons differentiable univariate real function
   */
  public static UnivariateDifferentiableFunction wrapDifferentiable(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new UnivariateDifferentiableFunction() {
      @Override
      public double value(double x) {
        return f.evaluate(x);
      }

      @Override
      public DerivativeStructure value(DerivativeStructure t) throws DimensionMismatchException {
        throw new IllegalArgumentException("Not implemented yet");
      }
    };
  }

}
