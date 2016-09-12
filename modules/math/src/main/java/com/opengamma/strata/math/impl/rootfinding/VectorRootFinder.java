/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import java.util.function.Function;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Parent class for root-finders that calculate a root for a vector function
 * (i.e. $\mathbf{y} = f(\mathbf{x})$, where $\mathbf{x}$ and $\mathbf{y}$ are vectors).
 */
public abstract class VectorRootFinder implements SingleRootFinder<DoubleArray, DoubleArray> {

  /**
   * {@inheritDoc}
   * Vector root finders only need a single starting point; if more than one is provided, the first is used and any other points ignored.
   */
  @Override
  public DoubleArray getRoot(Function<DoubleArray, DoubleArray> function, DoubleArray... startingPoint) {
    ArgChecker.notNull(startingPoint, "starting point");
    return getRoot(function, startingPoint[0]);
  }

  /**
   * @param function The (vector) function, not null
   * @param x0 The starting point, not null
   * @return The vector root of this function
   */
  public abstract DoubleArray getRoot(Function<DoubleArray, DoubleArray> function, DoubleArray x0);

  /**
   * @param function The function, not null
   * @param x0 The starting point, not null
   */
  protected void checkInputs(Function<DoubleArray, DoubleArray> function, DoubleArray x0) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(x0, "x0");
    int n = x0.size();
    for (int i = 0; i < n; i++) {
      if (!Doubles.isFinite(x0.get(i))) {
        throw new IllegalArgumentException("Invalid start position x0 = " + x0.toString());
      }
    }
    DoubleArray y = function.apply(x0);
    int m = y.size();
    for (int i = 0; i < m; i++) {
      if (!Doubles.isFinite(y.get(i))) {
        throw new IllegalArgumentException("Invalid start position f(x0) = " + y.toString());
      }
    }
  }

}
