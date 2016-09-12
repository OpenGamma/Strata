/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Class that calculates the real roots of a polynomial using Laguerre's method. This class is a wrapper for the
 * <a href="http://commons.apache.org/math/api-2.1/org/apache/commons/math/analysis/solvers/LaguerreSolver.html">Commons Math library implementation</a>
 * of Laguerre's method.
 */
//TODO Have a complex and real root finder
public class LaguerrePolynomialRealRootFinder implements Polynomial1DRootFinder<Double> {

  private static final LaguerreSolver ROOT_FINDER = new LaguerreSolver();
  private static final double EPS = 1e-16;

  /**
   * {@inheritDoc}
   * @throws MathException If there are no real roots; if the Commons method could not evaluate the function; if the Commons method could not converge.
   */
  @Override
  public Double[] getRoots(RealPolynomialFunction1D function) {
    ArgChecker.notNull(function, "function");
    try {
      Complex[] roots = ROOT_FINDER.solveAllComplex(function.getCoefficients(), 0);
      List<Double> realRoots = new ArrayList<>();
      for (Complex c : roots) {
        if (DoubleMath.fuzzyEquals(c.getImaginary(), 0d, EPS)) {
          realRoots.add(c.getReal());
        }
      }
      if (realRoots.isEmpty()) {
        throw new MathException("Could not find any real roots");
      }
      return realRoots.toArray(new Double[realRoots.size()]);
    } catch (TooManyEvaluationsException e) {
      throw new MathException(e);
    }
  }

}
