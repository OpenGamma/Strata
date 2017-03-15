/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.MathException;

/**
 * Differentiates a vector field (i.e. there is a vector value for every point
 * in some vector space) with respect to the vector space using finite difference.
 * <p>
 * For a function $\mathbf{y} = f(\mathbf{x})$ where $\mathbf{x}$ is a
 * n-dimensional vector and $\mathbf{y}$ is a m-dimensional vector, this class
 * produces the Jacobian function $\mathbf{J}(\mathbf{x})$, i.e. a function
 * that returns the Jacobian for each point $\mathbf{x}$, where
 * $\mathbf{J}$ is the $m \times n$ matrix $\frac{dy_i}{dx_j}$
 */
public class VectorFieldFirstOrderDifferentiator
    implements Differentiator<DoubleArray, DoubleArray, DoubleMatrix> {

  private static final double DEFAULT_EPS = 1e-5;

  private final double eps;
  private final double twoEps;
  private final FiniteDifferenceType differenceType;

  /**
   * Creates an instance using the default value of eps (10<sup>-5</sup>) and central differencing type.
   */
  public VectorFieldFirstOrderDifferentiator() {
    this(FiniteDifferenceType.CENTRAL, DEFAULT_EPS);
  }

  /**
   * Creates an instance using the default value of eps (10<sup>-5</sup>).
   * 
   * @param differenceType  the differencing type to be used in calculating the gradient function
   */
  public VectorFieldFirstOrderDifferentiator(FiniteDifferenceType differenceType) {
    this(differenceType, DEFAULT_EPS);
  }

  /**
   * Creates an instance using the central differencing type.
   * <p>
   * If the size of the domain is very small or very large, consider re-scaling first.
   * If this value is too small, the result will most likely be dominated by noise.
   * Use around 10<sup>-5</sup> times the domain size.
   * 
   * @param eps  the step size used to approximate the derivative
   */
  public VectorFieldFirstOrderDifferentiator(double eps) {
    this(FiniteDifferenceType.CENTRAL, eps);
  }

  /**
   * Creates an instance.
   * <p>
   * If the size of the domain is very small or very large, consider re-scaling first.
   * If this value is too small, the result will most likely be dominated by noise.
   * Use around 10<sup>-5</sup> times the domain size.
   * 
   * @param differenceType  the differencing type to be used in calculating the gradient function
   * @param eps  the step size used to approximate the derivative
   */
  public VectorFieldFirstOrderDifferentiator(FiniteDifferenceType differenceType, double eps) {
    ArgChecker.notNull(differenceType, "differenceType");
    this.differenceType = differenceType;
    this.eps = eps;
    this.twoEps = 2 * eps;
  }

  //-------------------------------------------------------------------------
  @Override
  public Function<DoubleArray, DoubleMatrix> differentiate(Function<DoubleArray, DoubleArray> function) {
    ArgChecker.notNull(function, "function");
    switch (differenceType) {
      case FORWARD:
        return new Function<DoubleArray, DoubleMatrix>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix apply(DoubleArray x) {
            ArgChecker.notNull(x, "x");
            DoubleArray y = function.apply(x);
            int n = x.size();
            int m = y.size();
            double[][] res = new double[m][n];
            for (int j = 0; j < n; j++) {
              double xj = x.get(j);
              DoubleArray up = function.apply(x.with(j, xj + eps));
              for (int i = 0; i < m; i++) {
                res[i][j] = (up.get(i) - y.get(i)) / eps;
              }
            }
            return DoubleMatrix.copyOf(res);
          }
        };
      case CENTRAL:
        return new Function<DoubleArray, DoubleMatrix>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix apply(DoubleArray x) {
            ArgChecker.notNull(x, "x");
            DoubleArray y = function.apply(x); // need this unused evaluation to get size of y
            int n = x.size();
            int m = y.size();
            double[][] res = new double[m][n];
            for (int j = 0; j < n; j++) {
              double xj = x.get(j);
              DoubleArray up = function.apply(x.with(j, xj + eps));
              DoubleArray down = function.apply(x.with(j, xj - eps));
              for (int i = 0; i < m; i++) {
                res[i][j] = (up.get(i) - down.get(i)) / twoEps;
              }
            }
            return DoubleMatrix.copyOf(res);
          }
        };
      case BACKWARD:
        return new Function<DoubleArray, DoubleMatrix>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix apply(DoubleArray x) {
            ArgChecker.notNull(x, "x");
            DoubleArray y = function.apply(x);
            int n = x.size();
            int m = y.size();
            double[][] res = new double[m][n];
            for (int j = 0; j < n; j++) {
              double xj = x.get(j);
              DoubleArray down = function.apply(x.with(j, xj - eps));
              for (int i = 0; i < m; i++) {
                res[i][j] = (y.get(i) - down.get(i)) / eps;
              }
            }
            return DoubleMatrix.copyOf(res);
          }
        };
      default:
        throw new IllegalArgumentException("Can only handle forward, backward and central differencing");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Function<DoubleArray, DoubleMatrix> differentiate(
      Function<DoubleArray, DoubleArray> function,
      Function<DoubleArray, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(domain, "domain");
    double[] wFwd = new double[] {-3., 4., -1.};
    double[] wCent = new double[] {-1., 0., 1.};
    double[] wBack = new double[] {1., -4., 3.};

    return new Function<DoubleArray, DoubleMatrix>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix apply(DoubleArray x) {
        ArgChecker.notNull(x, "x");
        ArgChecker.isTrue(domain.apply(x), "point {} is not in the function domain", x.toString());

        DoubleArray mid = function.apply(x); // need this unused evaluation to get size of y
        int n = x.size();
        int m = mid.size();
        double[][] res = new double[m][n];
        DoubleArray[] y = new DoubleArray[3];
        double[] w;

        for (int j = 0; j < n; j++) {
          double xj = x.get(j);
          DoubleArray xPlusOneEps = x.with(j, xj + eps);
          DoubleArray xMinusOneEps = x.with(j, xj - eps);
          if (!domain.apply(xPlusOneEps)) {
            DoubleArray xMinusTwoEps = x.with(j, xj - twoEps);
            if (!domain.apply(xMinusTwoEps)) {
              throw new MathException("cannot get derivative at point " + x.toString() + " in direction " + j);
            }
            y[2] = mid;
            y[0] = function.apply(xMinusTwoEps);
            y[1] = function.apply(xMinusOneEps);
            w = wBack;
          } else {
            if (!domain.apply(xMinusOneEps)) {
              y[0] = mid;
              y[1] = function.apply(xPlusOneEps);
              y[2] = function.apply(x.with(j, xj + twoEps));
              w = wFwd;
            } else {
              y[2] = function.apply(xPlusOneEps);
              y[0] = function.apply(xMinusOneEps);
              y[1] = mid;
              w = wCent;
            }
          }

          for (int i = 0; i < m; i++) {
            double sum = 0;
            for (int k = 0; k < 3; k++) {
              if (w[k] != 0.0) {
                sum += w[k] * y[k].get(i);
              }
            }
            res[i][j] = sum / twoEps;
          }
        }
        return DoubleMatrix.copyOf(res);
      }
    };
  }

}
