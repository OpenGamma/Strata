/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static org.testng.Assert.assertEquals;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.dataset.PriceIndexDataSets;

/**
 * Tests {@link PriceIndexInterpolatedCurve}.
 */
public class PriceIndexInterpolatedSeasonalityCurveTest {

  private static final Interpolator1D INTERPOLATOR_EXPONENTIAL =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(
          Interpolator1DFactory.EXPONENTIAL,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR,
          Interpolator1DFactory.EXPONENTIAL_EXTRAPOLATOR);

  private static final YearMonth VALUATION_MONTH = YearMonth.of(2015, 5);
  private static final double[] MONTHS_CURVE = new double[] {9.0, 21.0, 57.0, 117.0 };
  private static final LocalDateDoubleTimeSeries USCPI_TS = PriceIndexDataSets.USCPI_TS;
  private static final double[] VALUES = new double[] {240.500, 245.000, 265.000, 286.000 };
  private static final String NAME = "USD-HICP";
  private static final InterpolatedDoublesCurve INTERPOLATED_CURVE =
      InterpolatedDoublesCurve.from(MONTHS_CURVE, VALUES, INTERPOLATOR_EXPONENTIAL, NAME);
  private static final List<Double> SEASONALITY = new ArrayList<>();
  static {
    SEASONALITY.add(0.98);
    SEASONALITY.add(0.99);
    SEASONALITY.add(1.01);
    SEASONALITY.add(1.00);
    SEASONALITY.add(1.00);
    SEASONALITY.add(1.01);
    SEASONALITY.add(1.01);
    SEASONALITY.add(0.99);
    SEASONALITY.add(1.00);
    SEASONALITY.add(1.00);
    SEASONALITY.add(1.00);
    SEASONALITY.add(1.01);
  }
  private static final PriceIndexInterpolatedSeasonalityCurve PRICE_CURVE =
      PriceIndexInterpolatedSeasonalityCurve.of(VALUATION_MONTH, INTERPOLATED_CURVE, USCPI_TS, SEASONALITY);

  private static final YearMonth[] TEST_MONTHS = new YearMonth[] {YearMonth.of(2015, 1), YearMonth.of(2015, 5),
    YearMonth.of(2016, 5), YearMonth.of(2016, 6), YearMonth.of(2024, 12) };
  private static final double TOLERANCE_VALUE = 1.0E-10;
  private static final double TOLERANCE_DELTA = 1.0E-5;

  @Test
  public void test_of_wrong_seasonality_length() {
    assertThrowsIllegalArg(() -> PriceIndexInterpolatedSeasonalityCurve
        .of(VALUATION_MONTH, INTERPOLATED_CURVE, USCPI_TS, new ArrayList<>()));
  }
  
  @Test
  public void test_number_of_parameters() {
    assertEquals(MONTHS_CURVE.length, PRICE_CURVE.getParameterCount());
  }

  @Test
  public void test_name() {
    assertEquals(NAME, PRICE_CURVE.getName());
  }

  @Test
  public void test_price_index() {
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      YearMonth lastMonth = YearMonth.from(USCPI_TS.getLatestDate());
      double nbMonthLast = VALUATION_MONTH.until(lastMonth, MONTHS);
      double[] xExtended = new double[MONTHS_CURVE.length + 1];
      xExtended[0] = nbMonthLast;
      System.arraycopy(MONTHS_CURVE, 0, xExtended, 1, MONTHS_CURVE.length);
      double[] yExtended = new double[VALUES.length + 1];
      yExtended[0] = USCPI_TS.getLatestValue();
      System.arraycopy(VALUES, 0, yExtended, 1, VALUES.length);
      InterpolatedDoublesCurve finalCurve =
          new InterpolatedDoublesCurve(xExtended, yExtended, INTERPOLATOR_EXPONENTIAL, true, NAME);
      double nbMonth = VALUATION_MONTH.until(TEST_MONTHS[i], MONTHS);
      OptionalDouble valueTs = USCPI_TS.get(TEST_MONTHS[i].atEndOfMonth());
      double adj = SEASONALITY.get(TEST_MONTHS[i].getMonthValue() - 1);
      double valueExpected = valueTs.isPresent() ? valueTs.getAsDouble() : finalCurve.getYValue(nbMonth) * adj;
      double valueComputed = PRICE_CURVE.getPriceIndex(TEST_MONTHS[i]);
      assertEquals(valueExpected, valueComputed, TOLERANCE_VALUE);
    }
  }

  @Test
  public void test_shift_curve() {
    double[] shiftsAbsolute = new double[VALUES.length];
    shiftsAbsolute[1] += 1.00;
    shiftsAbsolute[3] += -2.00;
    List<ValueAdjustment> adjustments = new ArrayList<>();
    double[] shiftedValues = VALUES.clone();
    for (int i = 0; i < VALUES.length; i++) {
      shiftedValues[i] += shiftsAbsolute[i];
      adjustments.add(ValueAdjustment.ofDeltaAmount(shiftsAbsolute[i]));
    }
    InterpolatedDoublesCurve interpolatedShifted =
        InterpolatedDoublesCurve.from(MONTHS_CURVE, shiftedValues, INTERPOLATOR_EXPONENTIAL, NAME);
    PriceIndexInterpolatedSeasonalityCurve curveShiftedExpected =
        PriceIndexInterpolatedSeasonalityCurve.of(VALUATION_MONTH, interpolatedShifted, USCPI_TS, SEASONALITY);
    PriceIndexCurve curveShiftedComputed = PRICE_CURVE.shiftedBy(adjustments);
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      double valueExpected = curveShiftedExpected.getPriceIndex(TEST_MONTHS[i]);
      double valueComputed = curveShiftedComputed.getPriceIndex(TEST_MONTHS[i]);
      assertEquals(valueExpected, valueComputed, TOLERANCE_VALUE);
    }
  }

  @Test
  public void test_curve_sensitivity() {
    double shift = 0.0001;
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      double[] sensitivityComputed = PRICE_CURVE.getPriceIndexParameterSensitivity(TEST_MONTHS[i]);
      double[] sensitivityExpected = new double[VALUES.length];
      for (int j = 0; j < VALUES.length; j++) {
        double[] valueFd = new double[2];
        for (int k = 0; k < 2; k++) {
          List<ValueAdjustment> adjustments = new ArrayList<>();
          for (int l = 0; l < VALUES.length; l++) {
            adjustments.add(ValueAdjustment.ofDeltaAmount((l == j) ? ((k == 0) ? -shift : shift) : 0.0d));
          }
          PriceIndexCurve curveShifted = PRICE_CURVE.shiftedBy(adjustments);
          valueFd[k] = curveShifted.getPriceIndex(TEST_MONTHS[i]);
        }
        sensitivityExpected[j] = (valueFd[1] - valueFd[0]) / (2 * shift);
        assertEquals(sensitivityComputed[j], sensitivityExpected[j],
            TOLERANCE_DELTA, "Test: " + i + " - sensitivity: " + j);
      }
    }
  }
  
  @Test
  public void test_start_date_before_fixing() {
    double[] monthWrong = new double[] {-10.0, 21.0, 57.0, 117.0 };
    InterpolatedDoublesCurve interpolated =
        InterpolatedDoublesCurve.from(monthWrong, VALUES, INTERPOLATOR_EXPONENTIAL, NAME);
    assertThrowsIllegalArg(
        () -> PriceIndexInterpolatedSeasonalityCurve.of(VALUATION_MONTH, interpolated, USCPI_TS, SEASONALITY));
  }

}
