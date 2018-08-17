/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.linearalgebra.DecompositionResult;

/**
 * Contains the results of QR matrix decomposition.
 */
// CSOFF: AbbreviationAsWordInName
public interface QRDecompositionResult extends DecompositionResult {

  /**
   * Returns the matrix $\mathbf{R}$ of the decomposition.
   * <p>
   * $\mathbf{R}$ is an upper-triangular matrix.
   * @return the $\mathbf{R}$ matrix
   */
  public abstract DoubleMatrix getR();

  /**
   * Returns the matrix $\mathbf{Q}$ of the decomposition.
   * <p>
   * $\mathbf{Q}$ is an orthogonal matrix.
   * @return the $\mathbf{Q}$ matrix
   */
  public abstract DoubleMatrix getQ();

  /**
   * Returns the transpose of the matrix $\mathbf{Q}$ of the decomposition.
   * <p>
   * $\mathbf{Q}$ is an orthogonal matrix.
   * @return the $\mathbf{Q}$ matrix
   */
  public abstract DoubleMatrix getQT();

}
