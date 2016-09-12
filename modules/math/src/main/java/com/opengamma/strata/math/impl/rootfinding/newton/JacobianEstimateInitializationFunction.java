/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * 
 */
public class JacobianEstimateInitializationFunction implements NewtonRootFinderMatrixInitializationFunction {

  @Override
  public DoubleMatrix getInitializedMatrix(
      Function<DoubleArray, DoubleMatrix> jacobianFunction,
      DoubleArray x) {

    ArgChecker.notNull(jacobianFunction, "Jacobian Function");
    ArgChecker.notNull(x, "x");
    return jacobianFunction.apply(x);
  }

}
