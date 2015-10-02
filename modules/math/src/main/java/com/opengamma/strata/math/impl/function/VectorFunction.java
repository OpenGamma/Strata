/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Abstraction for the vector function $f: \mathbb{R}^m \to \mathbb{R}^n \quad x \mapsto f(x)$ where the 
 * Jacobian $j : \mathbb{R}^m \to \mathbb{R}^{n\times m} \quad x \mapsto j(x)$ is also provided 
 */
public abstract class VectorFunction extends Function1D<DoubleMatrix1D, DoubleMatrix1D> {

  /**
   * Calculate the Jacobian at a point $\mathbf{x}$. For a function 
   * $f: \mathbb{R}^m \to \mathbb{R}^n \quad x \mapsto f(x)$, the Jacobian is a n by m matrix
   * @param x The input vector $\mathbf{x}$
   * @return The Jacobian $\mathbf{J}$
   */
  public abstract DoubleMatrix2D calculateJacobian(final DoubleMatrix1D x);

  /**
   * The length of the input vector $\mathbf{x}$
   * @return length of input vector (domain) 
   */
  public abstract int getLengthOfDomain();

  /**
   * The length of the output vector $\mathbf{y}$
   * @return length of output vector (range) 
   */
  public abstract int getLengthOfRange();
}
