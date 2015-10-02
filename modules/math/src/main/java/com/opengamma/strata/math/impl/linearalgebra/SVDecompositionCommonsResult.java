/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Wrapper for results of the Commons implementation of singular value decomposition {@link SVDecompositionCommons}
 */
public class SVDecompositionCommonsResult implements SVDecompositionResult {
  private final double _condition;
  private final double _norm;
  private final int _rank;
  private final DoubleMatrix2D _s;
  private final double[] _singularValues;
  private final DoubleMatrix2D _u;
  private final DoubleMatrix2D _v;
  private final DoubleMatrix2D _uTranspose;
  private final DoubleMatrix2D _vTranspose;
  private final DecompositionSolver _solver;

  /**
   * @param svd The result of the SV decomposition, not null
   */
  public SVDecompositionCommonsResult(final SingularValueDecomposition svd) {
    ArgChecker.notNull(svd, "svd");
    _condition = svd.getConditionNumber();
    _norm = svd.getNorm();
    _rank = svd.getRank();
    _s = CommonsMathWrapper.unwrap(svd.getS());
    _singularValues = svd.getSingularValues();
    _u = CommonsMathWrapper.unwrap(svd.getU());
    _uTranspose = CommonsMathWrapper.unwrap(svd.getUT());
    _v = CommonsMathWrapper.unwrap(svd.getV());
    _vTranspose = CommonsMathWrapper.unwrap(svd.getVT());
    _solver = svd.getSolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getConditionNumber() {
    return _condition;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getNorm() {
    return _norm;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getRank() {
    return _rank;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getS() {
    return _s;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getSingularValues() {
    return _singularValues;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getU() {
    return _u;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getUT() {
    return _uTranspose;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getV() {
    return _v;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getVT() {
    return _vTranspose;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix1D solve(final DoubleMatrix1D b) {
    ArgChecker.notNull(b, "b");
    return CommonsMathWrapper.unwrap(_solver.solve(CommonsMathWrapper.wrap(b)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] solve(final double[] b) {
    ArgChecker.notNull(b, "b");
    return _solver.solve(new ArrayRealVector(b)).toArray();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D solve(final DoubleMatrix2D b) {
    ArgChecker.notNull(b, "b");
    return CommonsMathWrapper.unwrap(_solver.solve(CommonsMathWrapper.wrap(b)));
  }
}
