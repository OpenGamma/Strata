/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleArray;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix;

/**
 * 
 */
public interface NewtonRootFinderMatrixInitializationFunction {

  DoubleMatrix getInitializedMatrix(Function1D<DoubleArray, DoubleMatrix> jacobianFunction, DoubleArray x);

}
