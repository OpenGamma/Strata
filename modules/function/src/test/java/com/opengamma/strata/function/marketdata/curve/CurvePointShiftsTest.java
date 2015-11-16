/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

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
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.curve.perturb.CurvePointShift;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;

/**
 * Test {@link CurvePointShifts}.
 */
@Test
public class CurvePointShiftsTest {

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

    MarketDataBox<Curve> shiftedCurveBox = shift.applyTo(MarketDataBox.ofSingleValue(curve));

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
    List<SimpleCurveNodeMetadata> nodeMetadata = ImmutableList.of(
        SimpleCurveNodeMetadata.of(date(2011, 3, 8), TNR_1M),
        SimpleCurveNodeMetadata.of(date(2011, 5, 8), TNR_3M),
        SimpleCurveNodeMetadata.of(date(2011, 8, 8), TNR_6M));

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

    MarketDataBox<Curve> shiftedCurveBox = shift.applyTo(MarketDataBox.ofSingleValue(curve));

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

  public void noNodeMetadata() {
    Curve curve = InterpolatedNodalCurve.of(
        DefaultCurveMetadata.of("curve"),
        DoubleArray.of(1, 2, 3),
        DoubleArray.of(5, 6, 7),
        INTERPOLATOR);

    // use ImmutableMap to test coverage of builder.addShifts()
    ImmutableMap<Tenor, Double> map = ImmutableMap.of(Tenor.TENOR_1W, 0.1, Tenor.TENOR_1M, 0.2, Tenor.TENOR_3M, 0.3);
    CurvePointShifts shift = CurvePointShifts.builder(ShiftType.RELATIVE).addShifts(0, map).build();
    MarketDataBox<Curve> box = MarketDataBox.ofSingleValue(curve);

    assertThrows(() -> shift.applyTo(box), IllegalArgumentException.class, ".* no parameter metadata.*");
  }

  public void notNodalCurve() {
    CurveMetadata metadata = Curves.zeroRates(CurveName.of("curve"), DayCounts.ACT_365F, ImmutableList.of());
    Curve curve = new NonNodalCurve(metadata);

    CurvePointShift shift = CurvePointShift.builder(ShiftType.RELATIVE)
        .addShift(Tenor.TENOR_1W, 0.1)
        .addShift(Tenor.TENOR_1M, 0.2)
        .addShift(Tenor.TENOR_3M, 0.3)
        .build();

    assertThrows(() -> shift.applyTo(curve), UnsupportedOperationException.class, ".*NodalCurve.*");
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

  /**
   * Testing curve implementation.
   * <p>
   * Does not implement {@link NodalCurve}.
   */
  private static final class NonNodalCurve implements Curve {

    private final CurveMetadata metadata;

    public NonNodalCurve(CurveMetadata metadata) {
      this.metadata = metadata;
    }

    //-------------------------------------------------------------------------
    @Override
    public CurveMetadata getMetadata() {
      return metadata;
    }

    @Override
    public int getParameterCount() {
      throw new IllegalStateException();
    }

    @Override
    public double yValue(double x) {
      throw new IllegalStateException();
    }

    @Override
    public CurveUnitParameterSensitivity yValueParameterSensitivity(double x) {
      throw new IllegalStateException();
    }

    @Override
    public double firstDerivative(double x) {
      throw new IllegalStateException();
    }

  }
}
