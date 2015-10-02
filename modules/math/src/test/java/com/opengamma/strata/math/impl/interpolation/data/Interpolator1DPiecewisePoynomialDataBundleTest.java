/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.ClampedCubicSplineInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.ConstrainedCubicSplineInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator1D;

/**
 * 
 */
public class Interpolator1DPiecewisePoynomialDataBundleTest {

  /**
   * 
   */
  @Test
  public void hashCodeEqualsTest() {
    final double[] x = new double[] {1., 2., 3., 4., 5. };
    final double[] y = new double[] {1., 2., 3., 4., 5. };
    final double[] x1 = new double[] {1., 2., 3., 3.5, 5. };

    final PiecewisePolynomialInterpolator1D interp1 = new ConstrainedCubicSplineInterpolator1D();
    final PiecewisePolynomialInterpolator1D interp2 = new ConstrainedCubicSplineInterpolator1D();
    final PiecewisePolynomialInterpolator1D interp3 = new ClampedCubicSplineInterpolator1D();

    Interpolator1DPiecewisePoynomialDataBundle bundle1 = new Interpolator1DPiecewisePoynomialDataBundle(interp1.getDataBundle(x, y), interp1.getInterpolator());
    Interpolator1DPiecewisePoynomialDataBundle bundle2 = new Interpolator1DPiecewisePoynomialDataBundle(interp2.getDataBundle(x, y), interp2.getInterpolator());
    Interpolator1DPiecewisePoynomialDataBundle bundle3 = new Interpolator1DPiecewisePoynomialDataBundle(interp3.getDataBundle(x, y), interp3.getInterpolator());
    Interpolator1DPiecewisePoynomialDataBundle bundle4 = new Interpolator1DPiecewisePoynomialDataBundle(interp1.getDataBundle(x1, y), interp1.getInterpolator());

    assertTrue(bundle1.equals(bundle1));

    assertTrue(bundle1.equals(bundle2));
    assertTrue(bundle2.equals(bundle1));
    assertTrue(bundle1.hashCode() == bundle2.hashCode());

    assertTrue(!(bundle1.hashCode() == bundle3.hashCode()));
    assertTrue(!(bundle1.equals(bundle3)));
    assertTrue(!(bundle3.equals(bundle1)));

    assertTrue(!(bundle1.hashCode() == bundle4.hashCode()));
    assertTrue(!(bundle1.equals(bundle4)));
    assertTrue(!(bundle4.equals(bundle1)));

    assertTrue(!(bundle1.equals(null)));
    assertTrue(!(bundle1.equals(interp1)));
  }

}
