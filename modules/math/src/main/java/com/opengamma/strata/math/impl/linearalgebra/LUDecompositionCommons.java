/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * This class is a wrapper for the
 * <a href="http://commons.apache.org/proper/commons-math/javadocs/api-3.5/org/apache/commons/math3/linear/LUDecomposition.html">Commons Math3 library implementation</a> 
 * of LU decomposition.
 */
public class LUDecompositionCommons extends Decomposition<LUDecompositionResult> {

  @Override
  public LUDecompositionResult apply(DoubleMatrix x) {
    ArgChecker.notNull(x, "x");
    RealMatrix temp = CommonsMathWrapper.wrap(x);
    LUDecomposition lu = new LUDecomposition(temp);
    return new LUDecompositionCommonsResult(lu);
  }

}
