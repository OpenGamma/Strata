/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.apache.commons.math3.util.CombinatoricsUtils.binomialCoefficient;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * The k^th order difference matrix will act on a vector to produce the k^th order difference series. Related to this is
 * the k^th order finite difference differentiation matrix which acts on the values of a function evaluated at a
 * (non-uniformly spaced) set of points, to give the estimate of the k^th order derivative at those points (for unit
 * spaces points these are identical). One use of these matrices, is to provide matrices that penalise the gradient or 
 * curvature of data on a non-uniform grid. This should work for an arbitrary number of dimensions (for data that has
 * been flattened to a vector). 
 */
public abstract class PenaltyMatrixGenerator {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  //*******************************************************************************************
  // Difference methods 
  //*******************************************************************************************

  /**
   * get the k^th order difference matrix, D,  which acts on a vector, x, of length m to produce the k^th order
   * difference vector. The first k rows of D are set to zero - because of this D_1 * D_1 != D_2<p>
   * For example, for x = {x1,x2,....xn}, D_1 * x = {0, x2-x1, x3-x2,...., xn - x(n-1)} and
   * D_2 * x = {0,0, x3-2*x2+x1,....,xn-2*x(n-1)+x(n-2)} etc
   * @param m Length of the vector
   * @param k Difference order. Require m > k 
   * @return The k^th order difference matrix 
   */
  public static DoubleMatrix getDifferenceMatrix(int m, int k) {
    ArgChecker.notNegativeOrZero(m, "m");
    ArgChecker.notNegative(k, "k");
    ArgChecker.isTrue(k < m, "Difference order too high, require m > k, but have: m = {} and k = {}", m, k);
    if (k == 0) {
      return DoubleMatrix.identity(m);
    }
    int[] coeff = new int[k + 1];
    int sign = 1;
    for (int i = k; i >= 0; i--) {
      coeff[i] = (int) (sign * binomialCoefficient(k, i));
      sign = -sign;
    }

    double[][] data = new double[m][m];
    for (int i = k; i < m; i++) {
      for (int j = 0; j < k + 1; j++) {
        data[i][j + i - k] = coeff[j];
      }
    }
    return DoubleMatrix.ofUnsafe(data);
  }

  /**
   * get the k^th order penalty matrix, P. This is defined as P = (D^T)*D , where D is the k^th order difference
   * matrix (see getDifferenceMatrix), so the scalar amount (x^T)*P*x = |Dx|^2  is greater the more k^th order 
   * differences there are in the vector, x. This can then act as a penalty on x in some optimisation routine where
   * x is the vector of (fit) parameters. 
   * @param m Length of the  vector.
   * @param k Difference order. Require m > k 
   * @return The k^th order penalty matrix, P
   */
  public static DoubleMatrix getPenaltyMatrix(int m, int k) {
    ArgChecker.notNegativeOrZero(m, "m");
    ArgChecker.notNegative(k, "k");
    ArgChecker.isTrue(k < m, "Difference order too high, require m > k, but have: m = {} and k = {}", m, k);
    if (k == 0) {
      return DoubleMatrix.identity(m);
    }
    DoubleMatrix d = getDifferenceMatrix(m, k);
    DoubleMatrix dt = MA.getTranspose(d);
    return (DoubleMatrix) MA.multiply(dt, d);
  }

  /**
   * Assume a tensor has been flattened to a vector as {A_{0,0}, A_{0,1},...._A_{0,m}, A_{1,0}, A_{1,1},...._A_{1,m},...,A_{n,0}, A_{n,1},...._A_{n,m}}
   *  (see {@link #flattenMatrix}) that is, the <b>last</b> index changes most rapidly.  This produces a penalty matrix that acts on a given set of indexes only<P>
   * To produce a penalty matrix that acts on multiple indexes, produce one for each set of indexes and add them together (scaling if necessary)  
   * @param numElements The range of each index. In the example above, this would be {n,m} 
   * @param k Difference order. Require size[indices] > k
   * @param index Which set of indices does the matrix act on 
   * @return A penalty matrix 
   */
  public static DoubleMatrix getPenaltyMatrix(int[] numElements, int k, int index) {
    ArgChecker.notEmpty(numElements, "size");
    ArgChecker.isTrue(index >= 0 && index < numElements.length, "index must be in range 0 to {}", numElements.length);
    DoubleMatrix d = getPenaltyMatrix(numElements[index], k);
    return getMatrixForFlattened(numElements, d, index);
  }

  /**
   * Assume a tensor has been flattened to a vector as {A_{0,0}, A_{0,1},...._A_{0,m}, A_{1,0}, A_{1,1},...._A_{1,m},...,A_{n,0}, A_{n,1},...._A_{n,m}}
   * (see {@link #flattenMatrix}) that is, the <b>last</b> index changes most rapidly.  This produces the sum of penalty matrices (or order given by k) with each scaled 
   * by lambda. 
   * @param numElements The range of each index. In the example above, this would be {n,m} 
   * @param k The difference order for each dimension 
   * @param lambda The scaling for each dimension 
   * @return  A penalty matrix 
   */
  public static DoubleMatrix getPenaltyMatrix(int[] numElements, int[] k, double[] lambda) {
    ArgChecker.notEmpty(numElements, "size");
    ArgChecker.notEmpty(k, "k");
    ArgChecker.notEmpty(lambda, "lambda");
    int dim = numElements.length;
    ArgChecker.isTrue(dim == k.length, "k different length to size");
    ArgChecker.isTrue(dim == lambda.length, "lambda different lenght to size");

    DoubleMatrix p = (DoubleMatrix) MA.scale(getPenaltyMatrix(numElements, k[0], 0), lambda[0]);
    for (int i = 1; i < dim; i++) {
      DoubleMatrix temp = (DoubleMatrix) MA.scale(getPenaltyMatrix(numElements, k[i], i), lambda[i]);
      p = (DoubleMatrix) MA.add(p, temp);
    }
    return p;
  }

  //*******************************************************************************************
  // Finite difference methods for non-uniform grids 
  //*******************************************************************************************

  /**
   * Get the kth order finite difference derivative matrix, D_k(x),  for a non-uniform set of points. For a (1D) set
   * of points, x, (which are not necessarily uniformly spaced), and a set of values at those points, y (i.e. y=f(x))
   * the vector y_k = D_k(x) * y is the finite difference estimate of the k^th order derivative (d^k y/ dx^k) at
   * the points, x. <p>
   * K = 0, trivially return the identity matrix; for k = 1 and 2, this is a three point estimate. K > 2 is not implemented      
   * @param x A non-uniform set of points
   * @param k The order <b>Only first and second order are currently implemented</b>
   * @param includeEnds If true the matrix includes an estimate for the derivative at the end points; if false
   *   the first and last rows of the matrix are empty 
   * @return The derivative matrix
   */
  public static DoubleMatrix getDerivativeMatrix(double[] x, int k, boolean includeEnds) {
    ArgChecker.notEmpty(x, "x");
    ArgChecker.notNegative(k, "k");
    int size = x.length;
    ArgChecker.isTrue(k < size, "order too high. Length of x is {}, and k is {}", size, k);
    if (k == 0) {
      return DoubleMatrix.identity(size);
    } else if (k > 2) {
      throw new UnsupportedOperationException("cannot handle order (k) > 2");
    }
    ArgChecker.isTrue(size > 2, "Need at least 3 points for a three point estimate");
    double[] dx = new double[size - 1];
    double[] dx2 = new double[size - 1];
    for (int i = 0; i < (size - 1); i++) {
      double temp = x[i + 1] - x[i];
      ArgChecker.isTrue(temp > 0.0, "x not in ascending order, or two identical points");
      dx[i] = temp;
      dx2[i] = temp * temp;
    }
    double[] w = new double[size - 2];
    for (int i = 0; i < (size - 2); i++) {
      w[i] = 1.0 / (dx[i] * dx[i + 1] * (dx[i] + dx[i + 1]));
    }

    double[][] data = new double[size][size];
    if (k == 1) {
      for (int i = 1; i < (size - 1); i++) {
        data[i][i - 1] = -w[i - 1] * dx2[i];
        data[i][i] = w[i - 1] * (dx2[i] - dx2[i - 1]);
        data[i][i + 1] = w[i - 1] * dx2[i - 1];
      }
      //ends 
      if (includeEnds) {
        data[0][0] = -w[0] * dx[1] * (2 * dx[0] + dx[1]);
        data[0][1] = w[0] * (dx2[0] + dx2[1] + 2 * dx[0] * dx[1]);
        data[0][2] = -w[0] * dx2[0];
        data[size - 1][size - 3] = w[size - 3] * dx2[size - 2];
        data[size - 1][size - 2] = -w[size - 3] * (dx2[size - 3] + dx2[size - 2] + 2 * dx[size - 2] * dx[size - 3]);
        data[size - 1][size - 1] = w[size - 3] * dx[size - 3] * (2 * dx[size - 2] + dx[size - 3]);
      }
    } else {
      for (int i = 1; i < (size - 1); i++) {
        double tmp = 2 * w[i - 1];
        data[i][i - 1] = tmp * dx[i];
        data[i][i] = -tmp * (dx[i] + dx[i - 1]);
        data[i][i + 1] = tmp * dx[i - 1];
      }
      //ends 
      if (includeEnds) {
        data[0] = data[1];
        data[size - 1] = data[size - 2];
      }
    }
    return DoubleMatrix.copyOf(data);
  }

  /**
   * get a k^th order penalty matrix,P, for a non-uniform grid, x. $P = D^T D$ where D is the kth order finite difference
   * matrix (not including the ends). Given values y = f(x) at the grid points, y^T*P*y = |Dy|^2 is the sum of squares 
   * of the k^th order derivative estimates at the points.<p>
   * <b>Note:</b> In certain applications we may need an estimate of $\frac{1}{b-a}\int_a^b \left(\frac{d^k y}{dx^k}\right)^2 dx$ 
   * i.e. the RMS value of the k^th order derivative, between a & b - for a uniform x this forms a crude approximation. 
   * @param x A non-uniform set of points
   * @param k order The order <b>Only first and second order are currently implemented</b>
   * @return The k^th order penalty matrix
   */
  public static DoubleMatrix getPenaltyMatrix(double[] x, int k) {
    ArgChecker.notEmpty(x, "x");
    if (x.length == 1) {
      if (k == 0) {
        return DoubleMatrix.identity(1);
      }
      throw new IllegalArgumentException("order too high. Length of x is 1 and k is " + k);
    }
    double range = x[x.length - 1] - x[0];
    ArgChecker.notNegativeOrZero(range, "range of x");
    double scale = Math.pow(range, k);
    DoubleMatrix d = (DoubleMatrix) MA.scale(getDerivativeMatrix(x, k, false), scale);
    DoubleMatrix dt = MA.getTranspose(d);
    return (DoubleMatrix) MA.multiply(dt, d);
  }

  /**
   * Get a kth order penalty matrix for a non-uniform grid whose values have been flattened to a vector.
   * @param x the grid positions in each dimension 
   * @param k the (finite difference) order
   * @param index which index to act on 
   * @return a penalty matrix 
   */
  public static DoubleMatrix getPenaltyMatrix(double[][] x, int k, int index) {
    ArgChecker.noNulls(x, "x");
    //k is checked in call below
    int dim = x.length;
    int[] numElements = new int[dim];
    for (int i = 0; i < dim; i++) {
      numElements[i] = x[i].length;
    }
    DoubleMatrix p = getPenaltyMatrix(x[index], k);

    return getMatrixForFlattened(numElements, p, index);
  }

  /**
   * Get a penalty for a non-uniform grid whose values have been flattened to a vector. This
   * is the sum  of penalty matrices that act on each index, scaled by an amount lambda
   * @param x the grid positions in each dimension 
   * @param k the (finite difference) order in each dimension 
   * @param lambda the strength of the penalty in each dimension
   * @return a penalty matrix
   */
  public static DoubleMatrix getPenaltyMatrix(double[][] x, int[] k, double[] lambda) {
    ArgChecker.notEmpty(k, "k");
    //values of k are checked in calls to 1D getPenaltyMatrix
    ArgChecker.notEmpty(lambda, "lambda");
    int dim = x.length;
    ArgChecker.isTrue(dim == k.length, "k different lenght to size");
    ArgChecker.isTrue(dim == lambda.length, "lambda different length to size");

    DoubleMatrix p = (DoubleMatrix) MA.scale(getPenaltyMatrix(x, k[0], 0), lambda[0]);
    for (int i = 1; i < dim; i++) {
      DoubleMatrix temp = (DoubleMatrix) MA.scale(getPenaltyMatrix(x, k[i], i), lambda[i]);
      p = (DoubleMatrix) MA.add(p, temp);
    }
    return p;
  }

  /**
   * for a matrix {{A_{0,0}, A_{0,1},...._A_{0,m},{A_{1,0}, A_{1,1},...._A_{1,m},...,{A_{n,0}, A_{n,1},...._A_{n,m}}
   * flattened to a vector {A_{0,0}, A_{0,1},...._A_{0,m}, A_{1,0}, A_{1,1},...._A_{1,m},...,A_{n,0}, A_{n,1},...._A_{n,m}}.
   * @param aMatrix A matrix
   * @return a the flattened matrix 
   */
  public static DoubleArray flattenMatrix(DoubleMatrix aMatrix) {
    int elements = aMatrix.size();
    double[] data = new double[elements];
    int nRows = aMatrix.rowCount();
    int nCols = aMatrix.columnCount();
    int pos = 0;
    for (int i = 0; i < nRows; i++) {
      System.arraycopy(aMatrix.rowArray(i), 0, data, pos, nCols);
      pos += nCols;
    }
    return DoubleArray.copyOf(data);
  }

  /**
   * Assume a tensor has been flattened to a vector as {A_{0,0}, A_{0,1},...._A_{0,m}, A_{1,0}, A_{1,1},...._A_{1,m},...,A_{n,0}, A_{n,1},...._A_{n,m}}
   *  (see {@link #flattenMatrix}) that is, the <b>last</b> index changes most rapidly. 
   * Given a matrix, M, that acts on the elements of one index only, i.e.
   * $$y_{i, i_1, i_2, \dots,i_{k-1}, i_{k+1},\dots, i_n} =  \sum_{i_k=0}^{N_k-1} M_{i,i_k}  x_{i_1, i_2, \dots,i_k,\dots, i_n} $$
   * form the larger matrix that acts on the flattened vector.
   * @param numElements The number of elements in each index. In the example above, this would be {n,m} 
   * @param m the matrix M
   * @param index Which index does the matrix act on 
   * @return A (larger) matrix which acts on the flattened vector 
   */
  public static DoubleMatrix getMatrixForFlattened(int[] numElements, DoubleMatrix m, int index) {
    ArgChecker.notEmpty(numElements, "numElements");
    int dim = numElements.length;
    ArgChecker.notNull(m, "m");
    ArgChecker.isTrue(index >= 0 && index < dim, "indices outside range");
    ArgChecker.isTrue(m.columnCount() == numElements[index], "columns in m ({}) do not match numElements for index ({})", m.columnCount(), numElements[index]);
    int postProduct = 1;
    int preProduct = 1;
    for (int j = index + 1; j < dim; j++) {
      preProduct *= numElements[j];
    }
    for (int j = 0; j < index; j++) {
      postProduct *= numElements[j];
    }
    DoubleMatrix temp = m;
    if (preProduct != 1) {
      temp = (DoubleMatrix) MA.kroneckerProduct(temp, DoubleMatrix.identity(preProduct));
    }
    if (postProduct != 1) {
      temp = (DoubleMatrix) MA.kroneckerProduct(DoubleMatrix.identity(postProduct), temp);
    }

    return temp;
  }

}
