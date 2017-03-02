/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * The Vector field second order differentiator.
 */
public class VectorFieldSecondOrderDifferentiator implements Differentiator<DoubleArray, DoubleArray, DoubleMatrix[]> {

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
   * The tensor is represented as an array of DoubleMatrix, where each matrix is
   * a Hessian (for the dependent variable y_i), so the indexing is
   * H^i_{j,k} =\partial^2y_i/\partial x_j \partial x_k
   * 
   * @param function  the function representing the vector field
   * @return a function representing the second derivative of the vector field (i.e. a rank 3 tensor field)
   */
  @Override
  public Function<DoubleArray, DoubleMatrix[]> differentiate(
      Function<DoubleArray, DoubleArray> function) {

    ArgChecker.notNull(function, "function");
    Function<DoubleArray, DoubleMatrix> jacFunc = vectorFieldDiff.differentiate(function);
    Function<DoubleArray, DoubleMatrix[]> hFunc = maxtrixFieldDiff.differentiate(jacFunc);
    return new Function<DoubleArray, DoubleMatrix[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix[] apply(DoubleArray x) {
        DoubleMatrix[] gamma = hFunc.apply(x);
        return reshapeTensor(gamma);
      }
    };
  }

  //-------------------------------------------------------------------------
  @Override
  public Function<DoubleArray, DoubleMatrix[]> differentiate(
      Function<DoubleArray, DoubleArray> function,
      Function<DoubleArray, Boolean> domain) {

    ArgChecker.notNull(function, "function");
    Function<DoubleArray, DoubleMatrix> jacFunc = vectorFieldDiff.differentiate(function, domain);
    Function<DoubleArray, DoubleMatrix[]> hFunc = maxtrixFieldDiff.differentiate(jacFunc, domain);
    return new Function<DoubleArray, DoubleMatrix[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix[] apply(DoubleArray x) {
        DoubleMatrix[] gamma = hFunc.apply(x);
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
  private DoubleMatrix[] reshapeTensor(DoubleMatrix[] gamma) {
    int m = gamma.length;
    int n = gamma[0].rowCount();
    ArgChecker.isTrue(gamma[0].columnCount() == m,
        "tenor wrong size. Seond index is {}, should be {}", gamma[0].columnCount(), m);
    DoubleMatrix[] res = new DoubleMatrix[n];
    for (int i = 0; i < n; i++) {
      double[][] temp = new double[m][m];
      for (int j = 0; j < m; j++) {
        DoubleMatrix gammaJ = gamma[j];
        for (int k = j; k < m; k++) {
          temp[j][k] = gammaJ.get(i, k);
        }
      }
      for (int j = 0; j < m; j++) {
        for (int k = 0; k < j; k++) {
          temp[j][k] = temp[k][j];
        }
      }
      res[i] = DoubleMatrix.copyOf(temp);
    }
    return res;
  }

  //-------------------------------------------------------------------------
  public Function<DoubleArray, DoubleMatrix[]> differentiateFull(
      Function<DoubleArray, DoubleArray> function) {

    return new Function<DoubleArray, DoubleMatrix[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix[] apply(DoubleArray x) {
        ArgChecker.notNull(x, "x");
        DoubleArray y = function.apply(x);
        int n = x.size();
        int m = y.size();
        double[][][] res = new double[m][n][n];

        for (int j = 0; j < n; j++) {
          double xj = x.get(j);
          DoubleArray xPlusOneEps = x.with(j, xj + eps);
          DoubleArray xMinusOneEps = x.with(j, xj - eps);
          DoubleArray up = function.apply(x.with(j, xj + eps));
          DoubleArray down = function.apply(xMinusOneEps);
          for (int i = 0; i < m; i++) {
            res[i][j][j] = (up.get(i) + down.get(i) - 2 * y.get(i)) / epsSqr;
          }
          for (int k = j + 1; k < n; k++) {
            double xk = x.get(k);
            DoubleArray downup = function.apply(xMinusOneEps.with(k, xk + eps));
            DoubleArray downdown = function.apply(xMinusOneEps.with(k, xk - eps));
            DoubleArray updown = function.apply(xPlusOneEps.with(k, xk - eps));
            DoubleArray upup = function.apply(xPlusOneEps.with(k, xk + eps));
            for (int i = 0; i < m; i++) {
              res[i][j][k] = (upup.get(i) + downdown.get(i) - updown.get(i) - downup.get(i)) / 4 / epsSqr;
            }
          }
        }
        DoubleMatrix[] mres = new DoubleMatrix[m];
        for (int i = 0; i < m; i++) {
          for (int j = 0; j < n; j++) {
            for (int k = 0; k < j; k++) {
              res[i][j][k] = res[i][k][j];
            }
          }
          mres[i] = DoubleMatrix.copyOf(res[i]);
        }
        return mres;
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the second derivative of a vector field, without cross derivatives. 
   * 
   * This creates a function returning a matrix whose {i,j} element is 
   * $H^i_{j} = \partial^2y_i/\partial x_j \partial x_j$.
   * 
   * @param function  the function representing the vector field
   * @return a function representing the second derivative of the vector field (i.e. a rank 3 tensor field)
   */
  public Function<DoubleArray, DoubleMatrix> differentiateNoCross(Function<DoubleArray, DoubleArray> function) {

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
          DoubleArray down = function.apply(x.with(j, xj - eps));
          for (int i = 0; i < m; i++) {
            res[i][j] = (up.get(i) + down.get(i) - 2d * y.get(i)) / epsSqr;
          }
        }
        return DoubleMatrix.copyOf(res);
      }
    };
  }

}
