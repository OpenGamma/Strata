/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * 
 */
public class InverseJacobianDirectionFunction implements NewtonRootFinderDirectionFunction {

  private final MatrixAlgebra _algebra;

  public InverseJacobianDirectionFunction(MatrixAlgebra algebra) {
    ArgChecker.notNull(algebra, "algebra");
    _algebra = algebra;
  }

  @Override
  public DoubleArray getDirection(DoubleMatrix estimate, DoubleArray y) {
    ArgChecker.notNull(estimate, "estimate");
    ArgChecker.notNull(y, "y");
    return (DoubleArray) _algebra.multiply(estimate, y);
  }

}
