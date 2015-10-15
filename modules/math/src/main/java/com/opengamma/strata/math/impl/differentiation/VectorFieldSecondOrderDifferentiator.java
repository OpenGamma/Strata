/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * The Vector field second order differentiator.
 */
public class VectorFieldSecondOrderDifferentiator implements Differentiator<DoubleMatrix1D, DoubleMatrix1D, DoubleMatrix2D[]> {

  private static final double DEFAULT_EPS = 1e-4;

  private final double eps;
  private final double epsSqr;
  private final VectorFieldFirstOrderDifferentiator vectorFieldDiff;
  private final MatrixFieldFirstOrderDifferentiator maxtrixFieldDiff;

  /**
   * Creates an instance using the default values.
   */
  public VectorFieldSecondOrderDifferentiator() {
    this.eps = DEFAULT_EPS;
    this.epsSqr = eps * eps;
    this.vectorFieldDiff = new VectorFieldFirstOrderDifferentiator(eps);
    this.maxtrixFieldDiff = new MatrixFieldFirstOrderDifferentiator(eps);
  }

  //-------------------------------------------------------------------------
  /**
   * This computes the second derivative of a vector field, which is a rank 3 tensor field.
   * The tensor is represented as an array of DoubleMatrix2D, where each matrix is
   * a Hessian (for the dependent variable y_i), so the indexing is
   * H^i_{j,k} =\partial^2y_i/\partial x_j \partial x_k
   * 
   * @param function  the function representing the vector field
   * @return a function representing the second derivative of the vector field (i.e. a rank 3 tensor field)
   */
  @Override
  public Function1D<DoubleMatrix1D, DoubleMatrix2D[]> differentiate(
      Function1D<DoubleMatrix1D, DoubleMatrix1D> function) {

    ArgChecker.notNull(function, "function");
    Function1D<DoubleMatrix1D, DoubleMatrix2D> jacFunc = vectorFieldDiff.differentiate(function);
    Function1D<DoubleMatrix1D, DoubleMatrix2D[]> hFunc = maxtrixFieldDiff.differentiate(jacFunc);
    return new Function1D<DoubleMatrix1D, DoubleMatrix2D[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D[] evaluate(DoubleMatrix1D x) {
        DoubleMatrix2D[] gamma = hFunc.evaluate(x);
        return reshapeTensor(gamma);
      }
    };
  }

  //-------------------------------------------------------------------------
  @Override
  public Function1D<DoubleMatrix1D, DoubleMatrix2D[]> differentiate(
      Function1D<DoubleMatrix1D, DoubleMatrix1D> function,
      Function1D<DoubleMatrix1D, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    Function1D<DoubleMatrix1D, DoubleMatrix2D> jacFunc = vectorFieldDiff.differentiate(function, domain);
    Function1D<DoubleMatrix1D, DoubleMatrix2D[]> hFunc = maxtrixFieldDiff.differentiate(jacFunc, domain);
    return new Function1D<DoubleMatrix1D, DoubleMatrix2D[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D[] evaluate(DoubleMatrix1D x) {
        DoubleMatrix2D[] gamma = hFunc.evaluate(x);
        return reshapeTensor(gamma);
      }
    };
  }

  /**
   * Gamma is in the  form gamma^i_{j,k} =\partial^2y_j/\partial x_i \partial x_k, where i is the
   * index of the matrix in the stack (3rd index of the tensor), and j,k are the individual
   * matrix indices. We would like it in the form H^i_{j,k} =\partial^2y_i/\partial x_j \partial x_k,
   * so that each matrix is a Hessian (for the dependent variable y_i), hence the reshaping below.
   * 
   * @param gamma  the rank 3 tensor
   * @return the reshaped rank 3 tensor
   */
  private DoubleMatrix2D[] reshapeTensor(DoubleMatrix2D[] gamma) {
    int m = gamma.length;
    int n = gamma[0].rowCount();
    ArgChecker.isTrue(gamma[0].columnCount() == m,
        "tenor wrong size. Seond index is {}, should be {}", gamma[0].columnCount(), m);
    DoubleMatrix2D[] res = new DoubleMatrix2D[n];
    for (int i = 0; i < n; i++) {
      double[][] temp = new double[m][m];
      for (int j = 0; j < m; j++) {
        DoubleMatrix2D gammaJ = gamma[j];
        for (int k = j; k < m; k++) {
          temp[j][k] = gammaJ.get(i, k);
        }
      }
      for (int j = 0; j < m; j++) {
        for (int k = 0; k < j; k++) {
          temp[j][k] = temp[k][j];
        }
      }
      res[i] = DoubleMatrix2D.copyOf(temp);
    }
    return res;
  }

  //-------------------------------------------------------------------------
  public Function1D<DoubleMatrix1D, DoubleMatrix2D[]> differentiateFull(
      Function1D<DoubleMatrix1D, DoubleMatrix1D> function) {

    return new Function1D<DoubleMatrix1D, DoubleMatrix2D[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D[] evaluate(DoubleMatrix1D x) {
        ArgChecker.notNull(x, "x");
        DoubleMatrix1D y = function.evaluate(x);
        int n = x.size();
        int m = y.size();
        double[][][] res = new double[m][n][n];

        for (int j = 0; j < n; j++) {
          double xj = x.get(j);
          DoubleMatrix1D xPlusOneEps = x.with(j, xj + eps);
          DoubleMatrix1D xMinusOneEps = x.with(j, xj - eps);
          DoubleMatrix1D up = function.evaluate(x.with(j, xj + eps));
          DoubleMatrix1D down = function.evaluate(xMinusOneEps);
          for (int i = 0; i < m; i++) {
            res[i][j][j] = (up.get(i) + down.get(i) - 2 * y.get(i)) / epsSqr;
          }
          for (int k = j + 1; k < n; k++) {
            double xk = x.get(k);
            DoubleMatrix1D downup = function.evaluate(xMinusOneEps.with(k, xk + eps));
            DoubleMatrix1D downdown = function.evaluate(xMinusOneEps.with(k, xk - eps));
            DoubleMatrix1D updown = function.evaluate(xPlusOneEps.with(k, xk - eps));
            DoubleMatrix1D upup = function.evaluate(xPlusOneEps.with(k, xk + eps));
            for (int i = 0; i < m; i++) {
              res[i][j][k] = (upup.get(i) + downdown.get(i) - updown.get(i) - downup.get(i)) / 4 / epsSqr;
            }
          }
        }
        DoubleMatrix2D[] mres = new DoubleMatrix2D[m];
        for (int i = 0; i < m; i++) {
          for (int j = 0; j < n; j++) {
            for (int k = 0; k < j; k++) {
              res[i][j][k] = res[i][k][j];
            }
          }
          mres[i] = DoubleMatrix2D.copyOf(res[i]);
        }
        return mres;
      }
    };
  }

  //-------------------------------------------------------------------------
  public Function1D<DoubleMatrix1D, DoubleMatrix2D> differentiateNoCross(
      Function1D<DoubleMatrix1D, DoubleMatrix1D> function) {

    return new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
        ArgChecker.notNull(x, "x");
        DoubleMatrix1D y = function.evaluate(x);
        int n = x.size();
        int m = y.size();
        double[][] res = new double[m][n];
        for (int j = 0; j < n; j++) {
          double xj = x.get(j);
          DoubleMatrix1D up = function.evaluate(x.with(j, xj + eps));
          DoubleMatrix1D down = function.evaluate(x.with(j, xj - eps));
          for (int i = 0; i < m; i++) {
            res[i][j] = (up.get(i) + down.get(i) - 2 * y.get(i)) / epsSqr;
          }
        }
        return DoubleMatrix2D.copyOf(res);
      }
    };
  }

}
