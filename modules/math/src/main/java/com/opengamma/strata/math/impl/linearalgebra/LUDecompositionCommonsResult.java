/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Wrapper for results of the Commons implementation of LU decomposition ({@link LUDecompositionCommons})
 */
public class LUDecompositionCommonsResult implements LUDecompositionResult {
  private final double _determinant;
  private final DoubleMatrix _l;
  private final DoubleMatrix _p;
  private final int[] _pivot;
  private final DecompositionSolver _solver;
  private final DoubleMatrix _u;

  /**
   * @param lu The result of the LU decomposition, not null. $\mathbf{L}$ cannot be singular.
   */
  public LUDecompositionCommonsResult(LUDecomposition lu) {
    ArgChecker.notNull(lu, "LU decomposition");
    ArgChecker.notNull(lu.getL(), "Matrix is singular; could not perform LU decomposition");
    _determinant = lu.getDeterminant();
    _l = CommonsMathWrapper.unwrap(lu.getL());
    _p = CommonsMathWrapper.unwrap(lu.getP());
    _pivot = lu.getPivot();
    _solver = lu.getSolver();
    _u = CommonsMathWrapper.unwrap(lu.getU());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDeterminant() {
    return _determinant;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix getL() {
    return _l;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix getP() {
    return _p;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int[] getPivot() {
    return _pivot;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix getU() {
    return _u;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleArray solve(DoubleArray b) {
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
  public DoubleMatrix solve(DoubleMatrix b) {
    ArgChecker.notNull(b, "b");
    return CommonsMathWrapper.unwrap(_solver.solve(CommonsMathWrapper.wrap(b)));
  }

}
