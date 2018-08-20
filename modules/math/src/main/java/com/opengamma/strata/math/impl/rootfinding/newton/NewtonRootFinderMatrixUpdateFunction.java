/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * 
 */
//CSOFF: JavadocMethod
public interface NewtonRootFinderMatrixUpdateFunction {

  // TODO might be better to pass in NewtonVectorRootFinder.DataBundle as many of these arguments are not used.
  DoubleMatrix getUpdatedMatrix(
      Function<DoubleArray, DoubleMatrix> jacobianFunction,
      DoubleArray x,
      DoubleArray deltaX,
      DoubleArray deltaY,
      DoubleMatrix matrix);

}
