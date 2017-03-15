/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * This class is a wrapper for the <a href="http://commons.apache.org/math/api-2.1/org/apache/commons/math/linear/SingularValueDecompositionImpl.html">Commons Math library implementation</a>
 * of singular value decomposition.
 */
public class SVDecompositionCommons extends Decomposition<SVDecompositionResult> {

  @Override
  public SVDecompositionResult apply(DoubleMatrix x) {
    ArgChecker.notNull(x, "x");
    MatrixValidate.notNaNOrInfinite(x);
    RealMatrix commonsMatrix = CommonsMathWrapper.wrap(x);
    SingularValueDecomposition svd = new SingularValueDecomposition(commonsMatrix);
    return new SVDecompositionCommonsResult(svd);
  }

}
