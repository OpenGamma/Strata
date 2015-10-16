/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * A function from a vector x ({@link DoubleArray } to Boolean that returns true
 * iff all the elements of x are positive or zero.
 */
public class PositiveOrZero extends Function1D<DoubleArray, Boolean> {

  @Override
  public Boolean evaluate(DoubleArray x) {
    double[] data = x.toArray();
    for (double value : data) {
      if (value < 0.0) {
        return false;
      }
    }
    return true;
  }

}
