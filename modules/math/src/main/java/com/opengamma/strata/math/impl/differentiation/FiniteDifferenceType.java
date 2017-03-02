/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

/**
 * Enum representing the various differencing types that can be used to estimate the gradient of a function.
 * <p>
 * Forward: $\frac{f(x + \epsilon) - f(x)}{\epsilon}$
 * <p>
 * Central: $\frac{f(x + \epsilon) - f(x - \epsilon)}{2 * \epsilon}$
 * <p>
 * Backward: $\frac{f(x) - f(x - \epsilon)}{\epsilon}$
 */
public enum FiniteDifferenceType {

  /**
   * Forward differencing
   */
  FORWARD,
  /**
   * Central differencing
   */
  CENTRAL,
  /**
   * Backward differencing
   */
  BACKWARD

}
