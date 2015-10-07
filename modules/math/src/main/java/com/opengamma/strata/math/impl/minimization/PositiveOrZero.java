/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * A function from a vector x ({@link DoubleMatrix1D } to Boolean that returns true
 * iff all the elements of x are positive or zero.
 */
public class PositiveOrZero extends Function1D<DoubleMatrix1D, Boolean> {

  @Override
  public Boolean evaluate(DoubleMatrix1D x) {
    double[] data = x.getData();
    for (double value : data) {
      if (value < 0.0) {
        return false;
      }
    }
    return true;
  }

}
