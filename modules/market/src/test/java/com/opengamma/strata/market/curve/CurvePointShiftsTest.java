/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;

/**
 * Test {@link CurvePointShifts}.
 */
@Test
public class CurvePointShiftsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final String TNR_1W = "1W";
  private static final String TNR_1M = "1M";
  private static final String TNR_3M = "3M";
  private static final String TNR_6M = "6M";
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LOG_LINEAR;

  public void absolute() {
    List<LabelDateParameterMetadata> nodeMetadata = ImmutableList.of(
        LabelDateParameterMetadata.of(date(2011, 3, 8), TNR_1M),
        LabelDateParameterMetadata.of(date(2011, 5, 8), TNR_3M),
        LabelDateParameterMetadata.of(date(2011, 8, 8), TNR_6M));

    // This should create 4 scenarios. Scenario zero has no shifts and scenario 3 doesn't have shifts on all nodes
    CurvePointShifts shift = CurvePointShifts.builder(ShiftType.ABSOLUTE)
        .addShift(1, TNR_1W, 0.1) // Tenor not in the curve, should be ignored
        .addShift(1, TNR_1M, 0.2)
        .addShift(1, TNR_3M, 0.3)
        .addShift(2, TNR_1M, 0.4)
        .addShift(2, TNR_3M, 0.5)
        .addShift(2, TNR_6M, 0.6)
        .addShift(3, TNR_3M, 0.7)
        .build();

    Curve curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        INTERPOLATOR);

    MarketDataBox<Curve> shiftedCurveBox = shift.applyTo(MarketDataBox.ofSingleValue(curve), REF_DATA);

    Curve scenario1Curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5.2, 6.3, 7),
        INTERPOLATOR);

    Curve scenario2Curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5.4, 6.5, 7.6),
        INTERPOLATOR);

    Curve scenario3Curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6.7, 7),
        INTERPOLATOR);

    // Scenario zero has no perturbations so the expected curve is the same as the input
    List<Curve> expectedCurves = ImmutableList.of(curve, scenario1Curve, scenario2Curve, scenario3Curve);

    for (int scenarioIndex = 0; scenarioIndex < 4; scenarioIndex++) {
      // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
      for (int xIndex = 0; xIndex <= 40; xIndex++) {
        double xValue = xIndex * 0.1;
        Curve expectedCurve = expectedCurves.get(scenarioIndex);
        Curve shiftedCurve = shiftedCurveBox.getValue(scenarioIndex);
        double shiftedY = shiftedCurve.yValue(xValue);
        double expectedY = expectedCurve.yValue(xValue);
        assertThat(shiftedY)
            .overridingErrorMessage(
                "Curve differed in scenario %d at x value %f, expected %f, actual %f",
                scenarioIndex,
                xValue,
                expectedY,
                shiftedY)
            .isEqualTo(expectedY);
      }
    }
  }

  public void relative() {
    List<LabelDateParameterMetadata> nodeMetadata = ImmutableList.of(
        LabelDateParameterMetadata.of(date(2011, 3, 8), TNR_1M),
        LabelDateParameterMetadata.of(date(2011, 5, 8), TNR_3M),
        LabelDateParameterMetadata.of(date(2011, 8, 8), TNR_6M));

    // This should create 4 scenarios. Scenario zero has no shifts and scenario 3 doesn't have shifts on all nodes
    CurvePointShifts shift = CurvePointShifts.builder(ShiftType.RELATIVE)
        .addShift(1, TNR_1W, 0.1) // Tenor not in the curve, should be ignored
        .addShift(1, TNR_1M, 0.2)
        .addShift(1, TNR_3M, 0.3)
        .addShift(2, TNR_1M, 0.4)
        .addShift(2, TNR_3M, 0.5)
        .addShift(2, TNR_6M, 0.6)
        .addShift(3, TNR_3M, 0.7)
        .build();

    Curve curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        INTERPOLATOR);

    MarketDataBox<Curve> shiftedCurveBox = shift.applyTo(MarketDataBox.ofSingleValue(curve), REF_DATA);

    Curve scenario1Curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(6, 7.8, 7),
        INTERPOLATOR);

    Curve scenario2Curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(7, 9, 11.2),
        INTERPOLATOR);

    Curve scenario3Curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, nodeMetadata),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 10.2, 7),
        INTERPOLATOR);

    // Scenario zero has no perturbations so the expected curve is the same as the input
    List<Curve> expectedCurves = ImmutableList.of(curve, scenario1Curve, scenario2Curve, scenario3Curve);

    for (int scenarioIndex = 0; scenarioIndex < 4; scenarioIndex++) {
      // Check every point from 0 to 4 in steps of 0.1 is the same on the bumped curve and the expected curve
      for (int xIndex = 0; xIndex <= 40; xIndex++) {
        double xValue = xIndex * 0.1;
        Curve expectedCurve = expectedCurves.get(scenarioIndex);
        Curve shiftedCurve = shiftedCurveBox.getValue(scenarioIndex);
        double shiftedY = shiftedCurve.yValue(xValue);
        double expectedY = expectedCurve.yValue(xValue);
        assertThat(shiftedY)
            .overridingErrorMessage(
                "Curve differed in scenario %d at x value %f, expected %f, actual %f",
                scenarioIndex,
                xValue,
                expectedY,
                shiftedY)
            .isEqualTo(expectedY);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurvePointShifts test = CurvePointShifts.builder(ShiftType.RELATIVE)
        .addShift(0, Tenor.TENOR_1W, 0.1)
        .addShift(0, Tenor.TENOR_1M, 0.2)
        .addShift(0, Tenor.TENOR_3M, 0.3)
        .build();
    coverImmutableBean(test);
    CurvePointShifts test2 = CurvePointShifts.builder(ShiftType.ABSOLUTE)
        .addShift(0, Tenor.TENOR_1M, 0.2)
        .addShift(0, Tenor.TENOR_3M, 0.3)
        .build();
    coverBeanEquals(test, test2);
  }

}
