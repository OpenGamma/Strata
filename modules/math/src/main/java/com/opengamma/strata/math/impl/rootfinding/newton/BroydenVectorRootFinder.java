/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.linearalgebra.Decomposition;

/**
 * A root finder using Broyden's Jacobian update formula.
 */
public class BroydenVectorRootFinder extends BaseNewtonVectorRootFinder {

  /**
   * The default tolerance.
   */
  private static final double DEF_TOL = 1e-7;
  /**
   * The default maximum number of steps.
   */
  private static final int MAX_STEPS = 100;

  /**
   * Creates an instance.
   */
  public BroydenVectorRootFinder() {
    this(DEF_TOL, DEF_TOL, MAX_STEPS);
  }

  /**
   * Creates an instance.
   * 
   * @param decomp  the decomposition
   */
  public BroydenVectorRootFinder(Decomposition<?> decomp) {
    this(DEF_TOL, DEF_TOL, MAX_STEPS, decomp);
  }

  /**
   * Creates an instance.
   * 
   * @param absoluteTol  the absolute tolerance
   * @param relativeTol  the relative tolerance
   * @param maxSteps  the maximum steps
   */
  public BroydenVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps) {
    this(absoluteTol, relativeTol, maxSteps, new LUDecompositionCommons());
  }

  /**
   * Creates an instance.
   * 
   * @param absoluteTol  the absolute tolerance
   * @param relativeTol  the relative tolerance
   * @param maxSteps  the maximum steps
   * @param decomp  the decomposition
   */
  public BroydenVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps, Decomposition<?> decomp) {
    super(
        absoluteTol,
        relativeTol,
        maxSteps,
        new JacobianDirectionFunction(decomp),
        new JacobianEstimateInitializationFunction(),
        new BroydenMatrixUpdateFunction());
  }

}
