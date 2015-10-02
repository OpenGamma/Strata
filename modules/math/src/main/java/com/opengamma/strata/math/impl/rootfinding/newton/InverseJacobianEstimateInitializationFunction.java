/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrixUtils;

/**
 * 
 */
public class InverseJacobianEstimateInitializationFunction implements NewtonRootFinderMatrixInitializationFunction {
  private final Decomposition<?> _decomposition;

  public InverseJacobianEstimateInitializationFunction(final Decomposition<?> decomposition) {
    ArgChecker.notNull(decomposition, "decomposition");
    _decomposition = decomposition;
  }

  @Override
  public DoubleMatrix2D getInitializedMatrix(Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianFunction, final DoubleMatrix1D x) {
    ArgChecker.notNull(jacobianFunction, "jacobianFunction");
    ArgChecker.notNull(x, "x");
    final DoubleMatrix2D estimate = jacobianFunction.evaluate(x);
    final DecompositionResult decompositionResult = _decomposition.evaluate(estimate);
    return decompositionResult.solve(DoubleMatrixUtils.getIdentityMatrix2D(x.getNumberOfElements()));
  }

}
