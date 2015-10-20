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
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Wrapper for results of the Commons implementation of QR Decomposition ({@link QRDecompositionCommons}).
 */
public class QRDecompositionCommonsResult implements QRDecompositionResult {

  private final DoubleMatrix _q;
  private final DoubleMatrix _r;
  private final DoubleMatrix _qTranspose;
  private final DecompositionSolver _solver;

  /**
   * @param qr The result of the QR decomposition, not null
   */
  public QRDecompositionCommonsResult(QRDecomposition qr) {
    ArgChecker.notNull(qr, "qr");
    _q = CommonsMathWrapper.unwrap(qr.getQ());
    _r = CommonsMathWrapper.unwrap(qr.getR());
    _qTranspose = _q.transpose();
    _solver = qr.getSolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix getQ() {
    return _q;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix getQT() {
    return _qTranspose;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix getR() {
    return _r;
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
