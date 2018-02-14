/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.linearalgebra.Decomposition;

/**
 * A root finder that uses the Sherman-Morrison formula to invert Broyden's Jacobian update formula,
 * thus providing a direct update formula for the inverse Jacobian.
 */
public class ShermanMorrisonVectorRootFinder extends BaseNewtonVectorRootFinder {

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
  public ShermanMorrisonVectorRootFinder() {
    this(DEF_TOL, DEF_TOL, MAX_STEPS);
  }

  /**
   * Creates an instance.
   * 
   * @param absoluteTol  the absolute tolerance
   * @param relativeTol  the relative tolerance
   * @param maxSteps  the maximum steps
   */
  public ShermanMorrisonVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps) {
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
  public ShermanMorrisonVectorRootFinder(double absoluteTol, double relativeTol, int maxSteps, Decomposition<?> decomp) {
    this(absoluteTol, relativeTol, maxSteps, decomp, new OGMatrixAlgebra());
  }

  /**
   * Creates an instance.
   * 
   * @param absoluteTol  the absolute tolerance
   * @param relativeTol  the relative tolerance
   * @param maxSteps  the maximum steps
   * @param decomp  the decomposition
   * @param algebra  the instance of matrix algebra
   */
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
