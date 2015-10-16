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
public interface NewtonRootFinderMatrixInitializationFunction {

  DoubleMatrix getInitializedMatrix(Function1D<DoubleArray, DoubleMatrix> jacobianFunction, DoubleArray x);

}
