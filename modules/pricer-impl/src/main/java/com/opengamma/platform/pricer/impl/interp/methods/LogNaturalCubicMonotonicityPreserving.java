/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.interp.methods;

import com.opengamma.analytics.math.interpolation.LogNaturalSplineHelper;
import com.opengamma.analytics.math.interpolation.PiecewisePolynomialResult;
import com.opengamma.platform.pricer.impl.interp.PP_t;

public class LogNaturalCubicMonotonicityPreserving implements
    Interp1DMethodBacking {

  public static LogNaturalCubicMonotonicityPreserving s_instance = new LogNaturalCubicMonotonicityPreserving();

  @Override
  public double[] mutateYAsCtor(double[] y) {
    double[] mutatedY = new double[y.length];
    for (int i = 0; i < y.length; ++i) {
      mutatedY[i] = Math.log(y[i]);
    }
    return mutatedY;
  }

  @Override
  public PP_t assembler(double[] x, double[] y) {
    com.opengamma.analytics.math.interpolation.MonotonicityPreservingCubicSplineInterpolator impl =
        new com.opengamma.analytics.math.interpolation.MonotonicityPreservingCubicSplineInterpolator(
            new LogNaturalSplineHelper());
    PiecewisePolynomialResult bar = impl.interpolate(x, y);
    PP_t pp = new PP_t(bar.getKnots().getData(), bar.getCoefMatrix()
        .getData(), bar.getKnots().getData().length - 1, 4);
    return pp;
  }

  @Override
  public PP_t createpp(double[] x, double[] y) {
    assert (x.length == y.length);
    return assembler(x, mutateYAsCtor(y));
  }

  @Override
  public double mutatePPvalresultWith(double x) {
    return Math.exp(x);
  }

}
