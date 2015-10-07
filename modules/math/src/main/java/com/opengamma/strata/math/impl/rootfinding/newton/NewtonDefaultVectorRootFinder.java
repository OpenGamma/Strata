/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;

/**
 * Attempts to find the multi-dimensional root of a series of N equations with N variables, i.e. a square problem. 
 * If the analytic Jacobian is not known, it will be calculated using central difference 
 */
public class NewtonDefaultVectorRootFinder extends NewtonVectorRootFinder {

  private static final double DEF_TOL = 1e-7;
  private static final int MAX_STEPS = 100;

  public NewtonDefaultVectorRootFinder() {
    this(DEF_TOL, DEF_TOL, MAX_STEPS);
  }

  public NewtonDefaultVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps) {
    this(absoluteTol, relativeTol, maxSteps, new LUDecompositionCommons());
  }

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
