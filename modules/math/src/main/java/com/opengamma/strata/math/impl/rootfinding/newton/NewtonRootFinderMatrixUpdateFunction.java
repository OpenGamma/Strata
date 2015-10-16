/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * 
 */
public interface NewtonRootFinderMatrixUpdateFunction {

  // TODO might be better to pass in NewtonVectorRootFinder.DataBundle as many of these arguments are not used.
  DoubleMatrix getUpdatedMatrix(
      Function1D<DoubleArray, DoubleMatrix> jacobianFunction,
      DoubleArray x,
      DoubleArray deltaX,
      DoubleArray deltaY,
      DoubleMatrix matrix);

}
