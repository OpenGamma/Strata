/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * This class is a wrapper for the <a href="http://commons.apache.org/math/api-2.1/org/apache/commons/math/linear/QRDecompositionImpl.html">Commons Math library implementation</a> 
 * of QR decomposition.
 */
public class QRDecompositionCommons extends Decomposition<QRDecompositionResult> {

  @Override
  public QRDecompositionResult apply(DoubleMatrix x) {
    ArgChecker.notNull(x, "x");
    RealMatrix temp = CommonsMathWrapper.wrap(x);
    QRDecomposition qr = new QRDecomposition(temp);
    return new QRDecompositionCommonsResult(qr);
  }

}
