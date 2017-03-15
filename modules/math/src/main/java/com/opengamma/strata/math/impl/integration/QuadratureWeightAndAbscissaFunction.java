/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import com.opengamma.strata.math.impl.function.special.OrthogonalPolynomialFunctionGenerator;

/**
 * Interface for classes that generate weights and abscissas for use in Gaussian quadrature. The abscissas are the roots
 * of an orthogonal polynomial {@link OrthogonalPolynomialFunctionGenerator}.
 */
public interface QuadratureWeightAndAbscissaFunction {

  /**
   * @param n The number of weights and abscissas to generate, not negative or zero
   * @return An object containing the weights and abscissas
   */
  GaussianQuadratureData generate(int n);

}
