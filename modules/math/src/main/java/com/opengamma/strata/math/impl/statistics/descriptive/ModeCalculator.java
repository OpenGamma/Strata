/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;

/**
 * The mode of a series of data is the value that occurs more frequently in the data set.
 */
public class ModeCalculator implements Function<double[], Double> {
  private static final double EPS = 1e-16;

  //TODO more than one value can be the mode
  @Override
  public Double apply(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length > 0, "x cannot be empty");
    if (x.length == 1) {
      return x[0];
    }
    double[] x1 = Arrays.copyOf(x, x.length);
    Arrays.sort(x1);
    TreeMap<Integer, Double> counts = new TreeMap<>();
    int count = 1;
    for (int i = 1; i < x1.length; i++) {
      if (Math.abs(x1[i] - x1[i - 1]) < EPS) {
        count++;
      } else {
        counts.put(count, x1[i - 1]);
        count = 1;
      }
    }
    if (counts.lastKey() == 1) {
      throw new MathException("Could not find mode for array; no repeated values");
    }
    return counts.lastEntry().getValue();
  }

}
