/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DPiecewisePoynomialDataBundle;

/**
 * Piecewise Cubic Hermite Interpolating Polynomial (PCHIP)
 */
public class PCHIPInterpolator1D extends Interpolator1D {

  /** Serialization version */
  private static final long serialVersionUID = 1L;

  // TODO have options on method
  private static final PiecewisePolynomialFunction1D FUNC = new PiecewisePolynomialWithSensitivityFunction1D();

  @Override
  public Double interpolate(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialDataBundle);
    final Interpolator1DPiecewisePoynomialDataBundle polyData = (Interpolator1DPiecewisePoynomialDataBundle) data;
    final DoubleArray res = FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return res.get(0);
  }

  @Override
  public double firstDerivative(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialDataBundle);
    final Interpolator1DPiecewisePoynomialDataBundle polyData = (Interpolator1DPiecewisePoynomialDataBundle) data;
    final DoubleArray res = FUNC.differentiate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return res.get(0);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(final Interpolator1DDataBundle data, final Double value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, true), new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity());
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, true), new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity());
  }

}
