/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;

/**
 * 
 */
public class JacobianDirectionFunction implements NewtonRootFinderDirectionFunction {

  private final Decomposition<?> _decomposition;

  /**
   * Creates an instance.
   * 
   * @param decomposition  the decomposition
   */
  public JacobianDirectionFunction(Decomposition<?> decomposition) {
    ArgChecker.notNull(decomposition, "decomposition");
    _decomposition = decomposition;
  }

  @Override
  public DoubleArray getDirection(DoubleMatrix estimate, DoubleArray y) {
    ArgChecker.notNull(estimate, "estimate");
    ArgChecker.notNull(y, "y");
    DecompositionResult result = _decomposition.apply(estimate);
    return result.solve(y);
  }

}
