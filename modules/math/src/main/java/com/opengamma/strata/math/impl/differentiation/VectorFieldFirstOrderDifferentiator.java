/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

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
    implements Differentiator<DoubleMatrix1D, DoubleMatrix1D, DoubleMatrix2D> {

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
  public Function1D<DoubleMatrix1D, DoubleMatrix2D> differentiate(Function1D<DoubleMatrix1D, DoubleMatrix1D> function) {
    ArgChecker.notNull(function, "function");
    switch (differenceType) {
      case FORWARD:
        return new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
            ArgChecker.notNull(x, "x");
            DoubleMatrix1D y = function.evaluate(x);
            int n = x.getNumberOfElements();
            int m = y.getNumberOfElements();
            double[] xData = x.getData();
            double oldValue;
            double[][] res = new double[m][n];
            int i, j;
            DoubleMatrix1D up;
            for (j = 0; j < n; j++) {
              oldValue = xData[j];
              xData[j] += eps;
              up = function.evaluate(x);
              for (i = 0; i < m; i++) {
                res[i][j] = (up.getEntry(i) - y.getEntry(i)) / eps;
              }
              xData[j] = oldValue;
            }
            return new DoubleMatrix2D(res);
          }
        };
      case CENTRAL:
        return new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
            ArgChecker.notNull(x, "x");
            DoubleMatrix1D y = function.evaluate(x); // need this unused evaluation to get size of y
            int n = x.getNumberOfElements();
            int m = y.getNumberOfElements();
            double[] xData = x.getData();
            double oldValue;
            double[][] res = new double[m][n];
            int i, j;
            DoubleMatrix1D up, down;
            for (j = 0; j < n; j++) {
              oldValue = xData[j];
              xData[j] += eps;
              up = function.evaluate(x);
              xData[j] -= twoEps;
              down = function.evaluate(x);
              for (i = 0; i < m; i++) {
                res[i][j] = (up.getEntry(i) - down.getEntry(i)) / twoEps;
              }
              xData[j] = oldValue;
            }
            return new DoubleMatrix2D(res);
          }
        };
      case BACKWARD:
        return new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
            ArgChecker.notNull(x, "x");
            DoubleMatrix1D y = function.evaluate(x);
            int n = x.getNumberOfElements();
            int m = y.getNumberOfElements();
            double[] xData = x.getData();
            double oldValue;
            double[][] res = new double[m][n];
            int i, j;
            DoubleMatrix1D down;
            for (j = 0; j < n; j++) {
              oldValue = xData[j];
              xData[j] -= eps;
              down = function.evaluate(x);
              for (i = 0; i < m; i++) {
                res[i][j] = (y.getEntry(i) - down.getEntry(i)) / eps;
              }
              xData[j] = oldValue;
            }
            return new DoubleMatrix2D(res);
          }
        };
      default:
        throw new IllegalArgumentException("Can only handle forward, backward and central differencing");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<DoubleMatrix1D, DoubleMatrix2D> differentiate(
      Function1D<DoubleMatrix1D, DoubleMatrix1D> function,
      Function1D<DoubleMatrix1D, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(domain, "domain");
    double[] wFwd = new double[] {-3., 4., -1.};
    double[] wCent = new double[] {-1., 0., 1.};
    double[] wBack = new double[] {1., -4., 3.};

    return new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
        ArgChecker.notNull(x, "x");
        ArgChecker.isTrue(domain.evaluate(x), "point {} is not in the function domain", x.toString());

        DoubleMatrix1D mid = function.evaluate(x); // need this unused evaluation to get size of y
        int n = x.getNumberOfElements();
        int m = mid.getNumberOfElements();
        double[] xData = x.getData();
        double oldValue;
        double[][] res = new double[m][n];
        int i, j;
        DoubleMatrix1D[] y = new DoubleMatrix1D[3];
        double[] w;

        for (j = 0; j < n; j++) {
          oldValue = xData[j];
          xData[j] += eps;
          if (!domain.evaluate(x)) {
            xData[j] = oldValue - twoEps;
            if (!domain.evaluate(x)) {
              throw new MathException("cannot get derivative at point " + x.toString() + " in direction " + j);
            }
            y[2] = mid;
            y[0] = function.evaluate(x);
            xData[j] = oldValue - eps;
            y[1] = function.evaluate(x);
            w = wBack;
          } else {
            DoubleMatrix1D temp = function.evaluate(x);
            xData[j] = oldValue - eps;
            if (!domain.evaluate(x)) {
              y[0] = mid;
              y[1] = temp;
              xData[j] = oldValue + twoEps;
              y[2] = function.evaluate(x);
              w = wFwd;
            } else {
              y[2] = temp;
              xData[j] = oldValue - eps;
              y[0] = function.evaluate(x);
              y[1] = mid;
              w = wCent;
            }
          }

          for (i = 0; i < m; i++) {
            double sum = 0;
            for (int k = 0; k < 3; k++) {
              if (w[k] != 0.0) {
                sum += w[k] * y[k].getEntry(i);
              }
            }
            res[i][j] = sum / twoEps;
          }
          xData[j] = oldValue;
        }
        return new DoubleMatrix2D(res);
      }
    };
  }

}
