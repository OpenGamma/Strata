/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrixUtils;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Wrapper for results of the Commons implementation of QR Decomposition ({@link QRDecompositionCommons}).
 */
public class QRDecompositionCommonsResult implements QRDecompositionResult {
  private final DoubleMatrix2D _q;
  private final DoubleMatrix2D _r;
  private final DoubleMatrix2D _qTranspose;
  private final DecompositionSolver _solver;

  /**
   * @param qr The result of the QR decomposition, not null
   */
  public QRDecompositionCommonsResult(final QRDecomposition qr) {
    ArgChecker.notNull(qr, "qr");
    _q = CommonsMathWrapper.unwrap(qr.getQ());
    _r = CommonsMathWrapper.unwrap(qr.getR());
    _qTranspose = DoubleMatrixUtils.getTranspose(_q);
    _solver = qr.getSolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getQ() {
    return _q;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getQT() {
    return _qTranspose;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getR() {
    return _r;
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
