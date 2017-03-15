/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Weighting function based on {@code Math.sin}.
 */
final class SineWeightingFunction
    implements WeightingFunction {

  static final SineWeightingFunction INSTANCE = new SineWeightingFunction();

  private SineWeightingFunction() {
  }

  @Override
  public double getWeight(double y) {
    ArgChecker.inRangeInclusive(y, 0d, 1d, "y");
    return 0.5 * (Math.sin(Math.PI * (y - 0.5)) + 1);
  }

  @Override
  public String getName() {
    return "Sine";
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
    return "Sine weighting function";
  }

}
