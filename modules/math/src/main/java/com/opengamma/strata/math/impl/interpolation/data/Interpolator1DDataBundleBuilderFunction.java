/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * 
 */
public class Interpolator1DDataBundleBuilderFunction implements Function<DoubleArray, LinkedHashMap<String, Interpolator1DDataBundle>> {

  private final LinkedHashMap<String, double[]> _knotPoints;
  private final LinkedHashMap<String, Interpolator1D> _interpolators;
  private final int _nNodes;

  public Interpolator1DDataBundleBuilderFunction(LinkedHashMap<String, double[]> knotPoints, LinkedHashMap<String, Interpolator1D> interpolators) {
    ArgChecker.notNull(knotPoints, "null knot points");
    ArgChecker.notNull(interpolators, "null interpolators");
    int count = 0;
    for (Map.Entry<String, double[]> entry : knotPoints.entrySet()) {
      int size = entry.getValue().length;
      ArgChecker.isTrue(size > 0, "no knot points for " + entry.getKey());
      count += size;
    }
    _knotPoints = knotPoints;
    _interpolators = interpolators;
    _nNodes = count;
  }

  @Override
  public LinkedHashMap<String, Interpolator1DDataBundle> apply(DoubleArray x) {
    ArgChecker.notNull(x, "null data x");
    ArgChecker.isTrue(_nNodes == x.size(), "x wrong length");

    LinkedHashMap<String, Interpolator1DDataBundle> res = new LinkedHashMap<>();
    int index = 0;

    for (String name : _interpolators.keySet()) {
      Interpolator1D interpolator = _interpolators.get(name);
      double[] nodes = _knotPoints.get(name);
      double[] values = Arrays.copyOfRange(x.toArray(), index, index + nodes.length);
      index += nodes.length;
      Interpolator1DDataBundle db = interpolator.getDataBundleFromSortedArrays(nodes, values);
      res.put(name, db);
    }

    return res;
  }
}
