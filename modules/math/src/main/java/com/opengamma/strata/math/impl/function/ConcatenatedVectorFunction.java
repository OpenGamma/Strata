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
   * Creates an instance.
   * <p>
   * This creates the concatenated function, in the order that the sub functions are given.
   * 
   * @param functions  the sub functions 
   */
  public ConcatenatedVectorFunction(VectorFunction[] functions) {
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

  //-------------------------------------------------------------------------
  @Override
  public DoubleMatrix2D calculateJacobian(DoubleMatrix1D x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(
        x.size() == getLengthOfDomain(),
        "Incorrect length of x. Is {} but should be {}", x.size(), getLengthOfDomain());
    DoubleMatrix2D jac = DoubleMatrix2D.filled(getLengthOfRange(), getLengthOfDomain());

    int posInput = 0;
    int pos1 = 0;
    int pos2 = 0;
    for (int i = 0; i < _nPartitions; i++) {
      int nRows = _yPartition[i];
      int nCols = _xPartition[i];
      DoubleMatrix1D sub = x.subArray(posInput, posInput + nCols);
      DoubleMatrix2D subJac = _functions[i].calculateJacobian(sub);
      if (nCols > 0) {
        for (int r = 0; r < nRows; r++) {
          System.arraycopy(subJac.getData()[r], 0, jac.getData()[pos1++], pos2, nCols);
        }
        pos2 += nCols;
      } else {
        pos1 += nRows;
      }
      posInput += nCols;
    }
    return jac;
  }

  @Override
  public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(
        x.size() == getLengthOfDomain(),
        "Incorrect length of x. Is {} but should be {}", x.size(), getLengthOfDomain());
    double[] y = new double[getLengthOfRange()];
    int posInput = 0;
    int posOutput = 0;
    //evaluate each function (with the appropriate sub vector) and concatenate the results 
    for (int i = 0; i < _nPartitions; i++) {
      int length = _xPartition[i];
      DoubleMatrix1D sub = x.subArray(posInput, posInput + length);
      DoubleMatrix1D eval = _functions[i].evaluate(sub);
      eval.copyInto(y, posOutput);
      posInput += length;
      posOutput += eval.size();
    }
    return DoubleMatrix1D.copyOf(y);
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
