/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * 
 */
public class NewtonDefaultUpdateFunction implements NewtonRootFinderMatrixUpdateFunction {

  @Override
  public DoubleMatrix getUpdatedMatrix(
      Function1D<DoubleArray, DoubleMatrix> jacobianFunction,
      DoubleArray x,
      DoubleArray deltaX,
      DoubleArray deltaY,
      DoubleMatrix matrix) {

    ArgChecker.notNull(jacobianFunction, "jacobianFunction");
    ArgChecker.notNull(x, "x");
    return jacobianFunction.evaluate(x);
  }

}
