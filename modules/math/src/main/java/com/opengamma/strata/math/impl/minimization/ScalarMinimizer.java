/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import java.util.function.Function;

/**
 * Interface for classes that extend the functionality of {@link Minimizer} by providing
 * a method that allows the search area for the minimum to be bounded. 
 */
public interface ScalarMinimizer extends Minimizer<Function<Double, Double>, Double> {

  /**
   * @param function The function to minimize, not null
   * @param startPosition The start position
   * @param lowerBound The lower bound
   * @param upperBound The upper bound, must be greater than the upper bound
   * @return The minimum
   */
  double minimize(Function<Double, Double> function, double startPosition, double lowerBound, double upperBound);

}
