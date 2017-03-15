/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;

/**
 * A function to allow a smooth weighing between two functions.
 * <p>
 * If two functions f(x) and g(x) fit the data set (x_i,y_i) at the points x_a and x_b
 * (i.e. f(x_a) = g(x_a) = y_a and  f(x_b) = g(x_b) = y_b), then a weighted function
 * h(x) = w(x)f(x) + (1-w(x))*g(x) with 0 <= w(x) <= 1 will also fit the points a and b
 */
public interface WeightingFunction
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static WeightingFunction of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    if (uniqueName.equals(LinearWeightingFunction.INSTANCE.getName())) {
      return LinearWeightingFunction.INSTANCE;
    }
    if (uniqueName.equals(SineWeightingFunction.INSTANCE.getName())) {
      return SineWeightingFunction.INSTANCE;
    }
    throw new IllegalArgumentException("WeightingFunction name not found: " + uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the function weight for point x, based on the lower bound index.
   * 
   * @param xs  the independent data points
   * @param index  the index of the data point below x
   * @param x  the x-point to find the weight for
   * @return the weight
   */
  public default double getWeight(double[] xs, int index, double x) {
    ArgChecker.notNull(xs, "strikes");
    ArgChecker.notNegative(index, "index");
    ArgChecker.isTrue(index <= xs.length - 2, "index cannot be larger than {}, have {}", xs.length - 2, index);
    double y = (xs[index + 1] - x) / (xs[index + 1] - xs[index]);
    return getWeight(y);
  }

  /**
   * Gets the weight.
   * <p>
   * The condition that must be satisfied by all weight functions is that
   * w(1) = 1, w(0) = 0 and dw(y)/dy <= 0 - i.e. w(y) is monotonically decreasing.
   * 
   * @param y  a value between 0 and 1
   * @return the weight
   */
  public abstract double getWeight(double y);

}
