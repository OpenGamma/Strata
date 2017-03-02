/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Contains the results of QR matrix decomposition.
 */
public interface QRDecompositionResult extends DecompositionResult {

  /**
   * Returns the matrix $\mathbf{R}$ of the decomposition.
   * <p>
   * $\mathbf{R}$ is an upper-triangular matrix.
   * @return the $\mathbf{R}$ matrix
   */
  DoubleMatrix getR();

  /**
   * Returns the matrix $\mathbf{Q}$ of the decomposition.
   * <p>
   * $\mathbf{Q}$ is an orthogonal matrix.
   * @return the $\mathbf{Q}$ matrix
   */
  DoubleMatrix getQ();

  /**
   * Returns the transpose of the matrix $\mathbf{Q}$ of the decomposition.
   * <p>
   * $\mathbf{Q}$ is an orthogonal matrix.
   * @return the $\mathbf{Q}$ matrix
   */
  DoubleMatrix getQT();

}
