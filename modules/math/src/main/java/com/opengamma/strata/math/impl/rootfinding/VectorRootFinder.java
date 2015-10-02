/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Parent class for root-finders that calculate a root for a vector function (i.e. $\mathbf{y} = f(\mathbf{x})$, where
 * $\mathbf{x}$ and $\mathbf{y}$ are vectors).
 */
public abstract class VectorRootFinder implements SingleRootFinder<DoubleMatrix1D, DoubleMatrix1D> {

  /**
   * {@inheritDoc}
   * Vector root finders only need a single starting point; if more than one is provided, the first is used and any other points ignored.
   */
  @Override
  public DoubleMatrix1D getRoot(final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DoubleMatrix1D... startingPoint) {
    ArgChecker.notNull(startingPoint, "starting point");
    return getRoot(function, startingPoint[0]);
  }

  /**
   * @param function The (vector) function, not null
   * @param x0 The starting point, not null
   * @return The vector root of this function
   */
  public abstract DoubleMatrix1D getRoot(Function1D<DoubleMatrix1D, DoubleMatrix1D> function, DoubleMatrix1D x0);

  /**
   * @param function The function, not null
   * @param x0 The starting point, not null
   */
  protected void checkInputs(final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DoubleMatrix1D x0) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(x0, "x0");
    final int n = x0.getNumberOfElements();
    for (int i = 0; i < n; i++) {
      if (!Doubles.isFinite(x0.getEntry(i))) {
        throw new IllegalArgumentException("Invalid start position x0 = " + x0.toString());
      }
    }
    final DoubleMatrix1D y = function.evaluate(x0);
    final int m = y.getNumberOfElements();
    for (int i = 0; i < m; i++) {
      if (!Doubles.isFinite(y.getEntry(i))) {
        throw new IllegalArgumentException("Invalid start position f(x0) = " + y.toString());
      }
    }
  }

}
