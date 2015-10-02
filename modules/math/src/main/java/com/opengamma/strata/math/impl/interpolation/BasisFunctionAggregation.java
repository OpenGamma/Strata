/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.List;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * 
 * @param <T> The domain type of the function (e.g. Double, double[], DoubleMatrix1D etc) 
 */
public class BasisFunctionAggregation<T> extends Function1D<T, Double> {
  private final List<Function1D<T, Double>> _f;
  private final double[] _w;

  public BasisFunctionAggregation(final List<Function1D<T, Double>> functions, final double[] weights) {
    ArgChecker.notEmpty(functions, "no functions");
    ArgChecker.notNull(weights, "no weights");
    ArgChecker.isTrue(functions.size() == weights.length);
    _f = functions;
    _w = weights.clone();
  }

  @Override
  public Double evaluate(final T x) {
    ArgChecker.notNull(x, "x");
    double sum = 0;
    final int n = _w.length;
    for (int i = 0; i < n; i++) {
      final double temp = _f.get(i).evaluate(x);
      if (temp != 0.0) {
        sum += _w[i] * temp;
      }
    }
    return sum;
  }

  /**
   * The sensitivity of the value at a point x to the weights of the basis functions 
   * @param x value to be evaluated 
   * @return sensitivity w
   */
  public DoubleMatrix1D weightSensitivity(final T x) {
    ArgChecker.notNull(x, "x");
    final int n = _w.length;
    final DoubleMatrix1D res = new DoubleMatrix1D(n);
    final double[] data = res.getData();
    for (int i = 0; i < n; i++) {
      data[i] = _f.get(i).evaluate(x);
    }
    return res;
  }

  /**
   * The value of the function at the given point and its sensitivity to the weights of the basis functions
   * @param x value to be evaluated 
   * @return value and weight sensitivity 
   */
  public Pair<Double, DoubleMatrix1D> valueAndWeightSensitivity(final T x) {
    ArgChecker.notNull(x, "x");
    final int n = _w.length;
    double sum = 0;
    final DoubleMatrix1D sense = new DoubleMatrix1D(n);
    final double[] data = sense.getData();
    for (int i = 0; i < n; i++) {
      final double temp = _f.get(i).evaluate(x);
      if (temp != 0.0) {
        sum += _w[i] * temp;
        data[i] = temp;
      }
    }
    return Pair.of(sum, sense);
  }
}
