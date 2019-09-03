/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;

/**
 * Test {@link CurveParallelShifts}.
 */
public class CurveParallelShiftsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @Test
  public void test_absolute() {
    CurveParallelShifts test = CurveParallelShifts.absolute(1d, 2d, 4d);

    Curve baseCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates("curve", DayCounts.ACT_365F),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        CurveInterpolators.LOG_LINEAR);

    MarketDataBox<Curve> shiftedCurveBox = test.applyTo(MarketDataBox.ofSingleValue(baseCurve), REF_DATA);

    assertThat(shiftedCurveBox.getValue(0)).isEqualTo(ParallelShiftedCurve.absolute(baseCurve, 1d));
    assertThat(shiftedCurveBox.getValue(1)).isEqualTo(ParallelShiftedCurve.absolute(baseCurve, 2d));
    assertThat(shiftedCurveBox.getValue(2)).isEqualTo(ParallelShiftedCurve.absolute(baseCurve, 4d));
  }

  @Test
  public void test_relative() {
    CurveParallelShifts test = CurveParallelShifts.relative(0.1d, 0.2d, 0.4d);

    Curve baseCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates("curve", DayCounts.ACT_365F),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        CurveInterpolators.LOG_LINEAR);

    MarketDataBox<Curve> shiftedCurveBox = test.applyTo(MarketDataBox.ofSingleValue(baseCurve), REF_DATA);

    assertThat(shiftedCurveBox.getValue(0)).isEqualTo(ParallelShiftedCurve.relative(baseCurve, 0.1d));
    assertThat(shiftedCurveBox.getValue(1)).isEqualTo(ParallelShiftedCurve.relative(baseCurve, 0.2d));
    assertThat(shiftedCurveBox.getValue(2)).isEqualTo(ParallelShiftedCurve.relative(baseCurve, 0.4d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CurveParallelShifts test = CurveParallelShifts.absolute(1d, 2d, 4d);
    coverImmutableBean(test);
    CurveParallelShifts test2 = CurveParallelShifts.relative(2d, 3d, 4d);
    coverBeanEquals(test, test2);
  }

}
