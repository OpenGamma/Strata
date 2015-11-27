/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * A one-dimensional step interpolator using the upper bound value.
 * <P>The interpolated value of the function
 * <i>x</i> with <i>x<sub>1</sub> <= x < x<sub>2</sub></i> is given by: <i>y = y<sub>1</sub></i>
 */
public class StepUpperInterpolator1D extends Interpolator1D {
  private static final long serialVersionUID = 1L;

  @Override
  public double interpolate(final Interpolator1DDataBundle data, final double x) {
    ArgChecker.notNull(data, "data bundle");
    // For x equal to a key
    int index = data.indexOf(x);
    if (index >= 0) {
      return data.getIndex(index);
    }
    // For intermediary values, return the higher key value.
    return data.higherValue(x);
  }

  @Override
  public double firstDerivative(final Interpolator1DDataBundle data, final double x) {
    ArgChecker.notNull(data, "data bundle");
    return 0.;
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    return new ArrayInterpolator1DDataBundle(x, y);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    return new ArrayInterpolator1DDataBundle(x, y, true);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, double value) {
    return getFiniteDifferenceSensitivities(data, value);
  }

}
