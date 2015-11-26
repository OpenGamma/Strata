/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.interpolation.data.InterpolatorNDDataBundle;

/**
 * 
 */
public abstract class InterpolatorND {

  public abstract Double interpolate(InterpolatorNDDataBundle data, double[] x);

  protected void validateInput(InterpolatorNDDataBundle data, double[] x) {
    ArgChecker.notNull(x, "null position");
    ArgChecker.notNull(data, "null databundle");
    List<Pair<double[], Double>> rawData = data.getData();
    int dim = x.length;
    ArgChecker.isTrue(dim > 0, "0 dimension");
    ArgChecker.isTrue(rawData.get(0).getFirst().length == dim, "data and requested point different dimension");
  }

  public abstract InterpolatorNDDataBundle getDataBundle(double[] x, double[] y, double[] z, double[] values);

  public abstract InterpolatorNDDataBundle getDataBundle(List<Pair<double[], Double>> data);

  protected List<Pair<double[], Double>> transformData(double[] x, double[] y, double[] z, double[] values) {
    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(z, "z");
    ArgChecker.notNull(values, "values");
    int n = x.length;
    ArgChecker.isTrue(y.length == n, "number of ys {} is not equal to number of xs {}", y.length, n);
    ArgChecker.isTrue(z.length == n, "number of zs {} is not equal to number of xs {}", z.length, n);
    ArgChecker.isTrue(values.length == n, "number of values {} is not equal to number of xs {}", values.length, n);
    List<Pair<double[], Double>> data = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      data.add(Pair.of(new double[] {x[i], y[i], z[i] }, values[i]));
    }
    return data;
  }

  /**
   * @param data Interpolator data
   * @param x The co-ordinate at which to calculate the sensitivities.
   * @return The node sensitivities
   */
  public Map<double[], Double> getNodeSensitivitiesForValue(InterpolatorNDDataBundle data, double[] x) {
    throw new UnsupportedOperationException("Node sensitivities cannot be calculated by this interpolator");
  }
}
