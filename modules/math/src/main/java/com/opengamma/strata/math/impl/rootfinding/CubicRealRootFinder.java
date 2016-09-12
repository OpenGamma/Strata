/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import java.util.ArrayList;
import java.util.List;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.ComplexNumber;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Root finder that calculates the roots of a cubic equation using {@link CubicRootFinder}
 * and returns only the real roots. If there are no real roots, an exception is thrown.
 */
public class CubicRealRootFinder implements Polynomial1DRootFinder<Double> {

  private static final Double[] EMPTY_ARRAY = new Double[0];
  private static final Polynomial1DRootFinder<ComplexNumber> ROOT_FINDER = new CubicRootFinder();

  @Override
  public Double[] getRoots(RealPolynomialFunction1D function) {
    ArgChecker.notNull(function, "function");
    double[] coefficients = function.getCoefficients();
    if (coefficients.length != 4) {
      throw new IllegalArgumentException("Function is not a cubic");
    }
    ComplexNumber[] result = ROOT_FINDER.getRoots(function);
    List<Double> reals = new ArrayList<>();
    for (ComplexNumber c : result) {
      if (DoubleMath.fuzzyEquals(c.getImaginary(), 0d, 1e-16)) {
        reals.add(c.getReal());
      }
    }
    ArgChecker.isTrue(reals.size() > 0, "Could not find any real roots");
    return reals.toArray(EMPTY_ARRAY);
  }

}
