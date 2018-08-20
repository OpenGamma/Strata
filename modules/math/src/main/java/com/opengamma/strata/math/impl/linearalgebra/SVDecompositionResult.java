/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.linearalgebra.DecompositionResult;

/**
 * Contains the results of SV matrix decomposition.
 */
// CSOFF: AbbreviationAsWordInName
public interface SVDecompositionResult extends DecompositionResult {

  /**
   * Returns the matrix $\mathbf{U}$ of the decomposition.
   * <p>
   * $\mathbf{U}$ is an orthogonal matrix, i.e. its transpose is also its inverse.
   * @return the $\mathbf{U}$ matrix
   */
  public abstract DoubleMatrix getU();

  /**
   * Returns the transpose of the matrix $\mathbf{U}$ of the decomposition.
   * <p>
   * $\mathbf{U}$ is an orthogonal matrix, i.e. its transpose is also its inverse.
   * @return the U matrix (or null if decomposed matrix is singular)
   */
  public abstract DoubleMatrix getUT();

  /**
   * Returns the diagonal matrix $\mathbf{\Sigma}$ of the decomposition.
   * <p>
   * $\mathbf{\Sigma}$ is a diagonal matrix. The singular values are provided in
   * non-increasing order.
   * @return the $\mathbf{\Sigma}$ matrix
   */
  public abstract DoubleMatrix getS();

  /**
   * Returns the diagonal elements of the matrix $\mathbf{\Sigma}$ of the decomposition.
   * <p>
   * The singular values are provided in non-increasing order.
   * @return the diagonal elements of the $\mathbf{\Sigma}$ matrix
   */
  public abstract double[] getSingularValues();

  /**
   * Returns the matrix $\mathbf{V}$ of the decomposition.
   * <p>
   * $\mathbf{V}$ is an orthogonal matrix, i.e. its transpose is also its inverse.
   * @return the $\mathbf{V}$ matrix
   */
  public abstract DoubleMatrix getV();

  /**
   * Returns the transpose of the matrix $\mathbf{V}$ of the decomposition.
   * <p>
   * $\mathbf{V}$ is an orthogonal matrix, i.e. its transpose is also its inverse.
   * @return the $\mathbf{V}$ matrix
   */
  public abstract DoubleMatrix getVT();

  /**
   * Returns the $L_2$ norm of the matrix.
   * <p>
   * The $L_2$ norm is $\max\left(\frac{|\mathbf{A} \times U|_2}{|U|_2}\right)$, where $|.|_2$ denotes the vectorial 2-norm
   * (i.e. the traditional Euclidian norm).
   * @return norm
   */
  public abstract double getNorm();

  /**
   * Returns the condition number of the matrix.
   * @return condition number of the matrix
   */
  public abstract double getConditionNumber();

  /**
   * Returns the effective numerical matrix rank.
   * <p>The effective numerical rank is the number of non-negligible
   * singular values. The threshold used to identify non-negligible
   * terms is $\max(m, n) \times \mathrm{ulp}(S_1)$, where $\mathrm{ulp}(S_1)$  
   * is the least significant bit of the largest singular value.
   * @return effective numerical matrix rank
   */
  public abstract int getRank();

}
