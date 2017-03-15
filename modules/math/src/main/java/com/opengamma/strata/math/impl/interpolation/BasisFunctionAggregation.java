/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * 
 * @param <T> The domain type of the function (e.g. Double, double[], DoubleArray etc) 
 */
public class BasisFunctionAggregation<T> implements Function<T, Double> {

  private final List<Function<T, Double>> _f;
  private final double[] _w;

  /**
   * Creates an instance.
   * 
   * @param functions  the functions
   * @param weights  the weights
   */
  public BasisFunctionAggregation(List<Function<T, Double>> functions, double[] weights) {
    ArgChecker.notEmpty(functions, "no functions");
    ArgChecker.notNull(weights, "no weights");
    ArgChecker.isTrue(functions.size() == weights.length);
    _f = functions;
    _w = weights.clone();
  }

  @Override
  public Double apply(T x) {
    ArgChecker.notNull(x, "x");
    double sum = 0;
    int n = _w.length;
    for (int i = 0; i < n; i++) {
      double temp = _f.get(i).apply(x);
      if (temp != 0.0) {
        sum += _w[i] * temp;
      }
    }
    return sum;
  }

  /**
   * The sensitivity of the value at a point x to the weights of the basis functions.
   * 
   * @param x value to be evaluated 
   * @return sensitivity w
   */
  public DoubleArray weightSensitivity(T x) {
    ArgChecker.notNull(x, "x");
    return DoubleArray.of(_w.length, i -> _f.get(i).apply(x));
  }

  /**
   * The value of the function at the given point and its sensitivity to the weights of the basis functions.
   * 
   * @param x value to be evaluated 
   * @return value and weight sensitivity 
   */
  public Pair<Double, DoubleArray> valueAndWeightSensitivity(T x) {
    ArgChecker.notNull(x, "x");
    int n = _w.length;
    double sum = 0;
    double[] data = new double[n];
    for (int i = 0; i < n; i++) {
      double temp = _f.get(i).apply(x);
      if (temp != 0.0) {
        sum += _w[i] * temp;
        data[i] = temp;
      }
    }
    return Pair.of(sum, DoubleArray.ofUnsafe(data));
  }

}
