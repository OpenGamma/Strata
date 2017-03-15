/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Linear weighting function.
 */
final class LinearWeightingFunction
    implements WeightingFunction {

  static final LinearWeightingFunction INSTANCE = new LinearWeightingFunction();

  private LinearWeightingFunction() {
  }

  @Override
  public double getWeight(double y) {
    ArgChecker.inRangeInclusive(y, 0d, 1d, "y");
    return y;
  }

  @Override
  public String getName() {
    return "Linear";
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return "Linear weighting function";
  }

}
