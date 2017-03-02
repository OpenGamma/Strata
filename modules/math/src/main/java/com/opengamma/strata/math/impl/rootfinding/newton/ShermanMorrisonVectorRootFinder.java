/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Uses the Sherman-Morrison formula to invert Broyden's Jacobian update formula,
 * thus providing a direct update formula for the inverse Jacobian.
 */
public class ShermanMorrisonVectorRootFinder extends NewtonVectorRootFinder {

  private static final double DEF_TOL = 1e-7;
  private static final int MAX_STEPS = 100;

  public ShermanMorrisonVectorRootFinder() {
    this(DEF_TOL, DEF_TOL, MAX_STEPS);
  }

  public ShermanMorrisonVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps) {
    this(absoluteTol, relativeTol, maxSteps, new LUDecompositionCommons());
  }

  public ShermanMorrisonVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps, Decomposition<?> decomp) {
    this(absoluteTol, relativeTol, maxSteps, decomp, new OGMatrixAlgebra());
  }

  public ShermanMorrisonVectorRootFinder(
      double absoluteTol,
      double relativeTol,
      int maxSteps,
      Decomposition<?> decomp,
      MatrixAlgebra algebra) {

    super(
        absoluteTol,
        relativeTol,
        maxSteps,
        new InverseJacobianDirectionFunction(algebra),
        new InverseJacobianEstimateInitializationFunction(decomp),
        new ShermanMorrisonMatrixUpdateFunction(algebra));
  }

}
