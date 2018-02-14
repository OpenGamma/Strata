/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.linearalgebra.Decomposition;

/**
 * A root finder that attempts find the multi-dimensional root of a series of N equations with N variables (a square problem).
 * If the analytic Jacobian is not known, it will be calculated using central difference 
 */
public class NewtonDefaultVectorRootFinder extends BaseNewtonVectorRootFinder {

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
  public NewtonDefaultVectorRootFinder() {
    this(DEF_TOL, DEF_TOL, MAX_STEPS);
  }

  /**
   * Creates an instance.
   * 
   * @param absoluteTol  the absolute tolerance
   * @param relativeTol  the relative tolerance
   * @param maxSteps  the maximum steps
   */
  public NewtonDefaultVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps) {
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
  public NewtonDefaultVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps, Decomposition<?> decomp) {
    super(
        absoluteTol,
        relativeTol,
        maxSteps,
        new JacobianDirectionFunction(decomp),
        new JacobianEstimateInitializationFunction(),
        new NewtonDefaultUpdateFunction());
  }

}
