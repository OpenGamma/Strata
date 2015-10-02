/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.perturb;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.TestingCurve;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;

/**
 * Test {@link IndexedCurvePointShift}.
 */
@Test
public class IndexedCurvePointShiftTest {

  private static final CurveInterpolator INTERPOLATOR = new LogLinearInterpolator1D();

  public void absolute() {
    IndexedCurvePointShift shift = IndexedCurvePointShift.absolute(0, 0.1d);

    Curve curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F),
        new double[] {1, 2, 3},
        new double[] {5, 6, 7},
        INTERPOLATOR);

    Curve shiftedCurve = shift.applyTo(curve);

    Curve expectedCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F),
        new double[] {1, 2, 3},
        new double[] {5.1, 6, 7},
        INTERPOLATOR);

    // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
    for (int i = 0; i <= 40; i++) {
      double xValue = i * 0.1;
      assertThat(shiftedCurve.yValue(xValue)).isEqualTo(expectedCurve.yValue(xValue));
    }
  }

  public void relative() {
    IndexedCurvePointShift shift = IndexedCurvePointShift.relative(0, 0.1d);

    Curve curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F),
        new double[] {1, 2, 3},
        new double[] {5, 6, 7},
        INTERPOLATOR);

    Curve shiftedCurve = shift.applyTo(curve);

    Curve expectedCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F),
        new double[] {1, 2, 3},
        new double[] {5.5, 6, 7},
        INTERPOLATOR);

    // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
    for (int i = 0; i <= 40; i++) {
      double xValue = i * 0.1;
      assertThat(shiftedCurve.yValue(xValue)).isEqualTo(expectedCurve.yValue(xValue));
    }
  }

  public void notNodalCurve() {
    CurveMetadata metadata = Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, ImmutableList.of());
    Curve curve = new TestingCurve(metadata);

    IndexedCurvePointShift shift = IndexedCurvePointShift.absolute(0, 0.1d);

    assertThrows(() -> shift.applyTo(curve), UnsupportedOperationException.class, ".*NodalCurve.*");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IndexedCurvePointShift test = IndexedCurvePointShift.absolute(0, 0.1d);
    coverImmutableBean(test);
    IndexedCurvePointShift test2 = IndexedCurvePointShift.relative(2, 0.2d);
    coverBeanEquals(test, test2);
  }

}
