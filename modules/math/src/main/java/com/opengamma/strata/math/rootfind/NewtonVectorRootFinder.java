/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.rootfind;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;
import com.opengamma.strata.math.impl.rootfinding.newton.BroydenVectorRootFinder;
import com.opengamma.strata.math.linearalgebra.Decomposition;

/**
 * Performs Newton-Raphson style multi-dimensional root finding.
 * <p>
 * This uses the Jacobian matrix as a basis for some parts of the iterative process.
 */
public interface NewtonVectorRootFinder {

  /**
   * Obtains an instance of the Broyden root finder.
   * <p>
   * This uses SV decomposition and standard tolerances.
   * 
   * @return the root finder
   */
  public static NewtonVectorRootFinder broyden() {
    return new BroydenVectorRootFinder(new SVDecompositionCommons());
  }

  /**
   * Obtains an instance of the Broyden root finder specifying the tolerances.
   * <p>
   * This uses SV decomposition.
   * 
   * @param absoluteTol  the absolute tolerance
   * @param relativeTol  the relative tolerance
   * @param maxSteps  the maximum steps
   * @return the root finder
   */
  public static NewtonVectorRootFinder broyden(double absoluteTol, double relativeTol, int maxSteps) {
    return new BroydenVectorRootFinder(absoluteTol, relativeTol, maxSteps, new SVDecompositionCommons());
  }

  /**
   * Obtains an instance of the Broyden root finder specifying the tolerances.
   * 
   * @param absoluteTol  the absolute tolerance
   * @param relativeTol  the relative tolerance
   * @param maxSteps  the maximum steps
   * @param decomposition  the decomposition function
   * @return the root finder
   */
  public static NewtonVectorRootFinder broyden(
      double absoluteTol,
      double relativeTol,
      int maxSteps,
      Decomposition<?> decomposition) {

    return new BroydenVectorRootFinder(absoluteTol, relativeTol, maxSteps, decomposition);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the root from the specified start position.
   * <p>
   * This applies the specified function to find the root.
   * Note if multiple roots exist which one is found will depend on the start position.
   * 
   * @param function   the vector function
   * @param startPosition  the start position of the root finder for
   * @return the vector root of the collection of functions
   * @throws MathException if unable to find the root, such as if unable to converge
   */
  public abstract DoubleArray findRoot(Function<DoubleArray, DoubleArray> function, DoubleArray startPosition);

  /**
   * Finds the root from the specified start position.
   * <p>
   * This applies the specified function and Jacobian function to find the root.
   * Note if multiple roots exist which one is found will depend on the start position.
   * 
   * @param function   the vector function
   * @param jacobianFunction  the function to calculate the Jacobian
   * @param startPosition  the start position of the root finder for
   * @return the vector root of the collection of functions
   * @throws MathException if unable to find the root, such as if unable to converge
   */
  public abstract DoubleArray findRoot(
      Function<DoubleArray, DoubleArray> function,
      Function<DoubleArray, DoubleMatrix> jacobianFunction,
      DoubleArray startPosition);

}
