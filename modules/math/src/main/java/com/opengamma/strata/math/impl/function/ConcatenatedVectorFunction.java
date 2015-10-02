/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * For the set of $k$ vector functions $f_i: \mathbb{R}^{m_i} \to \mathbb{R}^{n_i} \quad x_i \mapsto f_i(x_i) = y_i$ 
 * this forms the function 
 * $f: \mathbb{R}^{m} \to \mathbb{R}^{n} \quad x_i \mapsto f(x) = y$ where $n = \sum_{i=1}^k n_i$ and  
 * $m = \sum_{i=1}^k m_i$ and $x = (x_1,x_2,\dots,x_k)$ \& $y = (y_1,y_2,\dots,y_k)$
 **/
public class ConcatenatedVectorFunction extends VectorFunction {

  private final int[] _xPartition;
  private final int[] _yPartition;
  private final int _nPartitions;
  private final VectorFunction[] _functions;
  private final int _sizeDom;
  private final int _sizeRange;

  /**
   * Form the concatenated function, in the order that the sub functions are given. 
   * @param functions The sub functions 
   */
  public ConcatenatedVectorFunction(final VectorFunction[] functions) {
    ArgChecker.noNulls(functions, "functions");
    _functions = functions;
    _nPartitions = functions.length;
    _xPartition = new int[_nPartitions];
    _yPartition = new int[_nPartitions];
    int m = 0;
    int n = 0;
    for (int i = 0; i < _nPartitions; i++) {
      _xPartition[i] = _functions[i].getLengthOfDomain();
      _yPartition[i] = _functions[i].getLengthOfRange();
      m += _xPartition[i];
      n += _yPartition[i];
    }
    _sizeDom = m;
    _sizeRange = n;
  }

  @Override
  public DoubleMatrix2D calculateJacobian(final DoubleMatrix1D x) {
    //arg check done by partition
    final DoubleMatrix1D[] subX = partition(x);
    final DoubleMatrix2D jac = new DoubleMatrix2D(getLengthOfRange(), getLengthOfDomain());

    int pos1 = 0;
    int pos2 = 0;
    for (int i = 0; i < _nPartitions; i++) {
      final DoubleMatrix2D subJac = _functions[i].calculateJacobian(subX[i]);
      final int nRows = _yPartition[i];
      final int nCols = _xPartition[i];
      if (nCols > 0) {
        for (int r = 0; r < nRows; r++) {
          System.arraycopy(subJac.getData()[r], 0, jac.getData()[pos1++], pos2, nCols);
        }
        pos2 += nCols;
      } else {
        pos1 += nRows;
      }
    }
    return jac;
  }

  @Override
  public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
    //arg check done by partition
    final DoubleMatrix1D[] subX = partition(x); //split the vector into sub vectors 
    final DoubleMatrix1D y = new DoubleMatrix1D(getLengthOfRange());
    int pos = 0;
    //evaluate each function (with the appropriate sub vector) and concatenate the results 
    for (int i = 0; i < _nPartitions; i++) {
      final double[] subY = _functions[i].evaluate(subX[i]).getData();
      final int length = subY.length;
      System.arraycopy(subY, 0, y.getData(), pos, length);
      pos += length;
    }
    return y;
  }

  /**
   * This splits a vectors into a number of sub vectors with lengths given by _xPartition
   * @param x The vector to be spit 
   * @return a set of sub vectors 
   */
  private DoubleMatrix1D[] partition(final DoubleMatrix1D x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.getNumberOfElements() == getLengthOfDomain(), "Incorrect length of x. Is {} but should be {}", x.getNumberOfElements(), getLengthOfDomain());
    final DoubleMatrix1D[] res = new DoubleMatrix1D[_nPartitions];
    int pos = 0;
    for (int i = 0; i < _nPartitions; i++) {
      final int length = _xPartition[i];
      res[i] = new DoubleMatrix1D(length);
      System.arraycopy(x.getData(), pos, res[i].getData(), 0, length);
      pos += length;
    }
    return res;
  }

  @Override
  public int getLengthOfDomain() {
    return _sizeDom;
  }

  @Override
  public int getLengthOfRange() {
    return _sizeRange;
  }

}
