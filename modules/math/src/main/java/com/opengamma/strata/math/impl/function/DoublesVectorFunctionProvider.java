/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.List;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;

/**
 * abstraction for anything that provides a {@link VectorFunction} for a set of data points (as Double)
 */
public abstract class DoublesVectorFunctionProvider implements VectorFunctionProvider<Double> {

  @Override
  public VectorFunction from(final List<Double> x) {
    ArgChecker.notNull(x, "x");
    return from(x.toArray(new Double[0]));
  }

  @Override
  public VectorFunction from(final Double[] x) {
    ArgChecker.notNull(x, "x");
    return from(DoubleArrayMath.toPrimitive(x));
  }

  /**
   * produce a vector function that depends in some way on the given data points 
   * @param x Array of data points
   * @return a {@link VectorFunction}
   */
  public abstract VectorFunction from(double[] x);

}
