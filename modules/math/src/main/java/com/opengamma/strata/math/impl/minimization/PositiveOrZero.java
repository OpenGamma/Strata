/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A function from a vector x ({@link DoubleArray } to Boolean that returns true
 * iff all the elements of x are positive or zero.
 */
public class PositiveOrZero implements Function<DoubleArray, Boolean> {

  @Override
  public Boolean apply(DoubleArray x) {
    double[] data = x.toArray();
    for (double value : data) {
      if (value < 0.0) {
        return false;
      }
    }
    return true;
  }

}
