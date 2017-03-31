/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;

/**
 * 
 */
public class InverseJacobianEstimateInitializationFunction implements NewtonRootFinderMatrixInitializationFunction {

  private final Decomposition<?> _decomposition;

  /**
   * Creates an instance.
   * 
   * @param decomposition  the decomposition
   */
  public InverseJacobianEstimateInitializationFunction(Decomposition<?> decomposition) {
    ArgChecker.notNull(decomposition, "decomposition");
    _decomposition = decomposition;
  }

  @Override
  public DoubleMatrix getInitializedMatrix(Function<DoubleArray, DoubleMatrix> jacobianFunction, DoubleArray x) {
    ArgChecker.notNull(jacobianFunction, "jacobianFunction");
    ArgChecker.notNull(x, "x");
    DoubleMatrix estimate = jacobianFunction.apply(x);
    DecompositionResult decompositionResult = _decomposition.apply(estimate);
    return decompositionResult.solve(DoubleMatrix.identity(x.size()));
  }

}
