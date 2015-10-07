/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Interface for classes that find the roots of a polynomial function {@link RealPolynomialFunction1D}.
 * Although the coefficients of the polynomial function must be real, the roots can be real or complex.
 * @param <T> Type of the roots.
 */
public interface Polynomial1DRootFinder<T> {

  /**
   * @param function The function, not null
   * @return The roots of the function
   */
  T[] getRoots(RealPolynomialFunction1D function);

}
