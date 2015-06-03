/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenarios.curves;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.ShiftType;
import com.opengamma.strata.market.curve.TenorCurveNodeMetadata;

@Test
public class CurvePointShiftTest {

  public void absolute() {
    List<TenorCurveNodeMetadata> nodeMetadata = ImmutableList.of(
        TenorCurveNodeMetadata.of(date(2011, 3, 8), Tenor.TENOR_1M),
        TenorCurveNodeMetadata.of(date(2011, 5, 8), Tenor.TENOR_3M),
        TenorCurveNodeMetadata.of(date(2011, 8, 8), Tenor.TENOR_6M));

    CurvePointShift shift = CurvePointShift.builder(ShiftType.ABSOLUTE)
        .addShift(Tenor.TENOR_1W, 0.1) // Tenor not in the curve, should be ignored
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();

    Curve curve = InterpolatedNodalCurve.of(
        CurveMetadata.of("curve", nodeMetadata),
        new double[]{1, 2, 3},
        new double[]{5, 6, 7},
        CurveInterpolator.of(Interpolator1DFactory.DOUBLE_QUADRATIC));

    Curve shiftedCurve = shift.apply(curve);

    Curve expectedCurve = InterpolatedNodalCurve.of(
        CurveMetadata.of("curve", nodeMetadata),
        new double[]{1, 2, 3},
        new double[]{5.2, 6.3, 7},
        CurveInterpolator.of(Interpolator1DFactory.DOUBLE_QUADRATIC));

    // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
    for (int i = 0; i <= 40; i++) {
      double xValue = i * 0.1;
      assertThat(shiftedCurve.yValue(xValue)).isEqualTo(expectedCurve.yValue(xValue));
    }
  }

  public void relative() {
    List<TenorCurveNodeMetadata> nodeMetadata = ImmutableList.of(
        TenorCurveNodeMetadata.of(date(2011, 3, 8), Tenor.TENOR_1M),
        TenorCurveNodeMetadata.of(date(2011, 5, 8), Tenor.TENOR_3M),
        TenorCurveNodeMetadata.of(date(2011, 8, 8), Tenor.TENOR_6M));

    CurvePointShift shift = CurvePointShift.builder(ShiftType.RELATIVE)
        .addShift(Tenor.TENOR_1W, 0.1) // Tenor not in the curve, should be ignored
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();

    Curve curve = InterpolatedNodalCurve.of(
        CurveMetadata.of("curve", nodeMetadata),
        new double[]{1, 2, 3},
        new double[]{5, 6, 7},
        CurveInterpolator.of(Interpolator1DFactory.DOUBLE_QUADRATIC));

    Curve shiftedCurve = shift.apply(curve);

    Curve expectedCurve = InterpolatedNodalCurve.of(
        CurveMetadata.of("curve", nodeMetadata),
        new double[]{1, 2, 3},
        new double[]{6, 7.8, 7},
        CurveInterpolator.of(Interpolator1DFactory.DOUBLE_QUADRATIC));

    // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
    for (int i = 0; i <= 40; i++) {
      double xValue = i * 0.1;
      assertThat(shiftedCurve.yValue(xValue)).isEqualTo(expectedCurve.yValue(xValue));
    }
  }

  public void noNodeMetadata() {
    Curve curve = InterpolatedNodalCurve.of(
        CurveMetadata.of("curve"),
        new double[]{1, 2, 3},
        new double[]{5, 6, 7},
        CurveInterpolator.of(Interpolator1DFactory.DOUBLE_QUADRATIC));

    CurvePointShift shift = CurvePointShift.builder(ShiftType.RELATIVE)
        .addShift(Tenor.TENOR_1W, 0.1)
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();

    assertThrows(() -> shift.apply(curve), IllegalArgumentException.class, ".* no parameter metadata.*");
  }

  public void notNodalCurve() {
    CurveMetadata metadata = CurveMetadata.of("curve", ImmutableList.of());
    Curve curve = mock(Curve.class);
    when(curve.getMetadata()).thenReturn(metadata);

    CurvePointShift shift = CurvePointShift.builder(ShiftType.RELATIVE)
        .addShift(Tenor.TENOR_1W, 0.1)
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();

    assertThrows(() -> shift.apply(curve), IllegalArgumentException.class, ".* can only be applied to NodalCurve.*");
  }
}
