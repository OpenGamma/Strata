/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

/**
 * Provides a null implementation of parameter transformation; the functions return unchanged values.
 */
public class NullTransform implements ParameterLimitsTransform {

  /**
   * Performs the null inverse transform {y -> y}
   * {@inheritDoc}
   */
  @Override
  public double inverseTransform(double y) {
    return y;
  }

  /**
   * The gradient of a null transform is one.
   * {@inheritDoc}
   */
  @Override
  public double inverseTransformGradient(double y) {
    return 1;
  }

  /**
   * Performs the null transform {x -> x}
   * {@inheritDoc}
   */
  @Override
  public double transform(double x) {
    return x;
  }

  /**
   * The gradient of a null transform is one
   * {@inheritDoc}
   */
  @Override
  public double transformGradient(double x) {
    return 1;
  }

  @Override
  public int hashCode() {
    return 37;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    return getClass() == obj.getClass();
  }

}
