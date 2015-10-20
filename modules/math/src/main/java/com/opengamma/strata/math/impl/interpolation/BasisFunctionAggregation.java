/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.List;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * 
 * @param <T> The domain type of the function (e.g. Double, double[], DoubleArray etc) 
 */
public class BasisFunctionAggregation<T> extends Function1D<T, Double> {

  private final List<Function1D<T, Double>> _f;
  private final double[] _w;

  public BasisFunctionAggregation(List<Function1D<T, Double>> functions, double[] weights) {
    ArgChecker.notEmpty(functions, "no functions");
    ArgChecker.notNull(weights, "no weights");
    ArgChecker.isTrue(functions.size() == weights.length);
    _f = functions;
    _w = weights.clone();
  }

  @Override
  public Double evaluate(T x) {
    ArgChecker.notNull(x, "x");
    double sum = 0;
    int n = _w.length;
    for (int i = 0; i < n; i++) {
      double temp = _f.get(i).evaluate(x);
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
    return DoubleArray.of(_w.length, i -> _f.get(i).evaluate(x));
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
      double temp = _f.get(i).evaluate(x);
      if (temp != 0.0) {
        sum += _w[i] * temp;
        data[i] = temp;
      }
    }
    return Pair.of(sum, DoubleArray.ofUnsafe(data));
  }

}
