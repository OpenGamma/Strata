/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * 
 */
public class InverseJacobianDirectionFunction implements NewtonRootFinderDirectionFunction {
  private final MatrixAlgebra _algebra;

  public InverseJacobianDirectionFunction(final MatrixAlgebra algebra) {
    ArgChecker.notNull(algebra, "algebra");
    _algebra = algebra;
  }

  @Override
  public DoubleMatrix1D getDirection(final DoubleMatrix2D estimate, final DoubleMatrix1D y) {
    ArgChecker.notNull(estimate, "estimate");
    ArgChecker.notNull(y, "y");
    return (DoubleMatrix1D) _algebra.multiply(estimate, y);
  }

}
