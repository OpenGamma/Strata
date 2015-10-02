/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * 
 */
public class NewtonDefaultUpdateFunction implements NewtonRootFinderMatrixUpdateFunction {

  @Override
  public DoubleMatrix2D getUpdatedMatrix(Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianFunction, DoubleMatrix1D x, final DoubleMatrix1D deltaX, final DoubleMatrix1D deltaY,
      final DoubleMatrix2D matrix) {
    ArgChecker.notNull(jacobianFunction, "jacobianFunction");
    ArgChecker.notNull(x, "x");
    return jacobianFunction.evaluate(x);
  }

}
