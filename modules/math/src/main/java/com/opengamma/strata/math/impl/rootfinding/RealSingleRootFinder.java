/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;

/**
 * Parent class for root-finders that find a single real root $x$ for a function $f(x)$.  
 */
public abstract class RealSingleRootFinder implements SingleRootFinder<Double, Double> {

  @Override
  public Double getRoot(Function<Double, Double> function, Double... startingPoints) {
    ArgChecker.notNull(startingPoints, "startingPoints");
    ArgChecker.isTrue(startingPoints.length == 2);
    return getRoot(function, startingPoints[0], startingPoints[1]);
  }

  public abstract Double getRoot(Function<Double, Double> function, Double x1, Double x2);

  /**
   * Tests that the inputs to the root-finder are not null, and that a root is bracketed by the bounding values.
   * 
   * @param function The function, not null
   * @param x1 The first bound, not null
   * @param x2 The second bound, not null, must be greater than x1
   * @throws IllegalArgumentException if x1 and x2 do not bracket a root
   */
  protected void checkInputs(Function<Double, Double> function, Double x1, Double x2) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(x1, "x1");
    ArgChecker.notNull(x2, "x2");
    ArgChecker.isTrue(x1 <= x2, "x1 must be less or equal to  x2");
    ArgChecker.isTrue(function.apply(x1) * function.apply(x2) <= 0, "x1 and x2 do not bracket a root");
  }

  /**
   * Tests that the inputs to the root-finder are not null, and that a root is bracketed by the bounding values.
   * 
   * @param function The function, not null
   * @param x1 The first bound, not null
   * @param x2 The second bound, not null, must be greater than x1
   * @throws IllegalArgumentException if x1 and x2 do not bracket a root
   */
  protected void checkInputs(DoubleFunction1D function, Double x1, Double x2) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(x1, "x1");
    ArgChecker.notNull(x2, "x2");
    ArgChecker.isTrue(x1 <= x2, "x1 must be less or equal to  x2");
    ArgChecker.isTrue(function.applyAsDouble(x1) * function.applyAsDouble(x2) <= 0, "x1 and x2 do not bracket a root");
  }

}
