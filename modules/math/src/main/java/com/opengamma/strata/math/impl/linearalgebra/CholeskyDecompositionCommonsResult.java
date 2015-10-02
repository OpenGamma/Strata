/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Wrapper for results of the Commons implementation of Cholesky decomposition ({@link CholeskyDecompositionCommons})
 */
public class CholeskyDecompositionCommonsResult implements CholeskyDecompositionResult {
  private final double _determinant;
  private final DoubleMatrix2D _l;
  private final DoubleMatrix2D _lt;
  private final DecompositionSolver _solver;

  /**
   * Constructor.
   * @param ch The result of the Cholesky decomposition.
   */
  public CholeskyDecompositionCommonsResult(final CholeskyDecomposition ch) {
    ArgChecker.notNull(ch, "Cholesky decomposition");
    _determinant = ch.getDeterminant();
    _l = CommonsMathWrapper.unwrap(ch.getL());
    _lt = CommonsMathWrapper.unwrap(ch.getLT());
    _solver = ch.getSolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix1D solve(DoubleMatrix1D b) {
    ArgChecker.notNull(b, "b");
    return CommonsMathWrapper.unwrap(_solver.solve(CommonsMathWrapper.wrap(b)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] solve(double[] b) {
    ArgChecker.notNull(b, "b");
    return _solver.solve(new ArrayRealVector(b)).toArray();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D solve(DoubleMatrix2D b) {
    ArgChecker.notNull(b, "b");
    return CommonsMathWrapper.unwrap(_solver.solve(CommonsMathWrapper.wrap(b)));
  }

  @Override
  public DoubleMatrix2D getL() {
    return _l;
  }

  @Override
  public DoubleMatrix2D getLT() {
    return _lt;
  }

  @Override
  public double getDeterminant() {
    return _determinant;
  }

}
