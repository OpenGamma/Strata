/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.perturb;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.curve.TestingCurve;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;

/**
 * Test {@link CurvePointShift}.
 */
@Test
public class CurvePointShiftTest {

  private static final String TNR_1W = "1W";
  private static final String TNR_1M = "1M";
  private static final String TNR_3M = "3M";
  private static final String TNR_6M = "6M";
  private static final CurveInterpolator INTERPOLATOR = new LogLinearInterpolator1D();

  public void absolute() {
    List<SimpleCurveNodeMetadata> nodeMetadata = ImmutableList.of(
        SimpleCurveNodeMetadata.of(date(2011, 3, 8), TNR_1M),
        SimpleCurveNodeMetadata.of(date(2011, 5, 8), TNR_3M),
        SimpleCurveNodeMetadata.of(date(2011, 8, 8), TNR_6M));

    CurvePointShift shift = CurvePointShift.builder(ShiftType.ABSOLUTE)
        .addShift(TNR_1W, 0.1) // Tenor not in the curve, should be ignored
        .addShift(TNR_1M, 0.2) // shift based on identifier
        .addShift(TNR_3M, 0.3) // shift based on label
        .build();

    Curve curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        INTERPOLATOR);

    Curve shiftedCurve = shift.applyTo(curve);

    Curve expectedCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5.2, 6.3, 7),
        INTERPOLATOR);

    // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
    for (int i = 0; i <= 40; i++) {
      double xValue = i * 0.1;
      assertThat(shiftedCurve.yValue(xValue)).isEqualTo(expectedCurve.yValue(xValue));
    }
  }

  public void relative() {
    List<SimpleCurveNodeMetadata> nodeMetadata = ImmutableList.of(
        SimpleCurveNodeMetadata.of(date(2011, 3, 8), TNR_1M),
        SimpleCurveNodeMetadata.of(date(2011, 5, 8), TNR_3M),
        SimpleCurveNodeMetadata.of(date(2011, 8, 8), TNR_6M));

    CurvePointShift shift = CurvePointShift.builder(ShiftType.RELATIVE)
        .addShift(TNR_1W, 0.1) // Tenor not in the curve, should be ignored
        .addShift(TNR_1M, 0.2)
        .addShift(TNR_3M, 0.3)
        .build();

    Curve curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        INTERPOLATOR);

    Curve shiftedCurve = shift.applyTo(curve);

    Curve expectedCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(6, 7.8, 7),
        INTERPOLATOR);

    // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
    for (int i = 0; i <= 40; i++) {
      double xValue = i * 0.1;
      assertThat(shiftedCurve.yValue(xValue)).isEqualTo(expectedCurve.yValue(xValue));
    }
  }

  public void noNodeMetadata() {
    Curve curve = InterpolatedNodalCurve.of(
        DefaultCurveMetadata.of("curve"),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        INTERPOLATOR);

    // use ImmutableMap to test coverage of builder.addShifts()
    ImmutableMap<Tenor, Double> map = ImmutableMap.of(Tenor.TENOR_1W, 0.1, Tenor.TENOR_1M, 0.2, Tenor.TENOR_3M, 0.3);
    CurvePointShift shift = CurvePointShift.builder(ShiftType.RELATIVE).addShifts(map).build();

    assertThrows(() -> shift.applyTo(curve), IllegalArgumentException.class, ".* no parameter metadata.*");
  }

  public void notNodalCurve() {
    CurveMetadata metadata = Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, ImmutableList.of());
    Curve curve = new TestingCurve(metadata);

    CurvePointShift shift = CurvePointShift.builder(ShiftType.RELATIVE)
        .addShift(Tenor.TENOR_1W, 0.1)
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();

    assertThrows(() -> shift.applyTo(curve), UnsupportedOperationException.class, ".*NodalCurve.*");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurvePointShift test = CurvePointShift.builder(ShiftType.RELATIVE)
        .addShift(Tenor.TENOR_1W, 0.1)
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();
    coverImmutableBean(test);
    CurvePointShift test2 = CurvePointShift.builder(ShiftType.ABSOLUTE)
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();
    coverBeanEquals(test, test2);
  }

}
