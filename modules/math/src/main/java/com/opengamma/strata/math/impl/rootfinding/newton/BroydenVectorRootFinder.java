/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;

/**
 *  Uses Broyden's Jacobian update formula
 */
public class BroydenVectorRootFinder extends NewtonVectorRootFinder {

  private static final double DEF_TOL = 1e-7;
  private static final int MAX_STEPS = 100;

  public BroydenVectorRootFinder() {
    this(DEF_TOL, DEF_TOL, MAX_STEPS);
  }

  public BroydenVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps) {
    this(absoluteTol, relativeTol, maxSteps, new LUDecompositionCommons());
  }

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
