/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * 
 */
public interface NewtonRootFinderMatrixInitializationFunction {

  DoubleMatrix2D getInitializedMatrix(Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianFunction, DoubleMatrix1D x);
}
