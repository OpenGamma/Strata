/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * This class is a wrapper for the <a href="http://commons.apache.org/proper/commons-math/javadocs/api-3.5/org/apache/commons/math3/linear/LUDecomposition.html">Commons Math3 library implementation</a> 
 * of LU decomposition.
 */
public class LUDecompositionCommons extends Decomposition<LUDecompositionResult> {

  /**
   * {@inheritDoc}
   */
  @Override
  public LUDecompositionResult evaluate(final DoubleMatrix2D x) {
    ArgChecker.notNull(x, "x");
    final RealMatrix temp = CommonsMathWrapper.wrap(x);
    final LUDecomposition lu = new LUDecomposition(temp);
    return new LUDecompositionCommonsResult(lu);
  }

}
