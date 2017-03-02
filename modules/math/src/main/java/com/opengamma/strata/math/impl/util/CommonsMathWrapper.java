/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import java.util.function.Function;

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
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.ComplexNumber;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;

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

  /**
   * Wraps a function.
   * 
   * @param f  an OG 1-D function mapping doubles onto doubles
   * @return a Commons univariate real function
   */
  public static MultivariateFunction wrapMultivariate(Function<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return point -> f.apply(point[0]);
  }

  /**
   * Wraps a function.
   * 
   * @param f  an OG 1-D function mapping vectors of doubles onto doubles
   * @return a Commons multivariate real function
   */
  public static MultivariateFunction wrapMultivariateVector(Function<DoubleArray, Double> f) {
    ArgChecker.notNull(f, "f");
    return point -> f.apply(DoubleArray.copyOf(point));
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
  public static Function<Double, Double> unwrap(PolynomialFunctionLagrangeForm lagrange) {
    ArgChecker.notNull(lagrange, "lagrange");
    return new Function<Double, Double>() {

      @Override
      public Double apply(Double x) {
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
        return f.applyAsDouble(x);
      }

      @Override
      public DerivativeStructure value(DerivativeStructure t) throws DimensionMismatchException {
        throw new IllegalArgumentException("Not implemented yet");
      }
    };
  }

}
