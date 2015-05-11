/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static org.testng.Assert.assertEquals;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.dataset.PriceIndexDataSets;

/**
 * Tests {@link PriceIndexInterpolatedCurve}.
 */
public class PriceIndexInterpolatedCurveTest {

  private static final Interpolator1D INTERPOLATOR_EXPONENTIAL =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(
          Interpolator1DFactory.EXPONENTIAL,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR,
          Interpolator1DFactory.EXPONENTIAL_EXTRAPOLATOR);

  private static final YearMonth VALUATION_MONTH = YearMonth.of(2015, 5);
  private static final double[] MONTHS_CURVE = new double[] {-4.0, -3.0, -2.0, 12.0, 24.0, 60.0, 120.0 };
  private static final LocalDateDoubleTimeSeries USCPI_TS = PriceIndexDataSets.USCPI_TS;
  private static final double[] VALUES = new double[] {USCPI_TS.get(LocalDate.of(2015, 1, 31)).getAsDouble(),
    USCPI_TS.get(LocalDate.of(2015, 2, 28)).getAsDouble(), USCPI_TS.get(LocalDate.of(2015, 3, 31)).getAsDouble(),
    240.500, 245.000, 265.000, 286.000 };
  private static final String NAME = "USD-HICP";
  private static final InterpolatedDoublesCurve INTERPOLATED_CURVE =
      InterpolatedDoublesCurve.from(MONTHS_CURVE, VALUES, INTERPOLATOR_EXPONENTIAL, NAME);
  private static final PriceIndexInterpolatedCurve PRICE_CURVE =
      PriceIndexInterpolatedCurve.of(INTERPOLATED_CURVE, VALUATION_MONTH);

  private static final YearMonth[] TEST_MONTHS = new YearMonth[] {YearMonth.of(2015, 1), YearMonth.of(2015, 5),
    YearMonth.of(2016, 5), YearMonth.of(2016, 6), YearMonth.of(2024, 12) };
  private static final double TOLERANCE_VALUE = 1.0E-10;
  private static final double TOLERANCE_DELTA = 1.0E-5;

  @Test
  public void test_number_of_parameters() {
    assertEquals(MONTHS_CURVE.length, PRICE_CURVE.getNumberOfParameters());
  }

  @Test
  public void test_name() {
    assertEquals(NAME, PRICE_CURVE.getName());
  }

  @Test
  public void test_price_index() {
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      double nbMonth = VALUATION_MONTH.until(TEST_MONTHS[i], MONTHS);
      double valueExpected = INTERPOLATED_CURVE.getYValue(nbMonth);
      double valueComputed = PRICE_CURVE.getPriceIndex(TEST_MONTHS[i]);
      assertEquals(valueExpected, valueComputed, TOLERANCE_VALUE);
    }
  }

  @Test
  public void test_shift_curve() {
    double[] shifts = new double[VALUES.length];
    shifts[3] += 1.00;
    shifts[5] += -2.00;
    double[] shiftedValues = VALUES.clone();
    for (int i = 0; i < VALUES.length; i++) {
      shiftedValues[i] += shifts[i];
    }
    InterpolatedDoublesCurve interpolatedShifted =
        InterpolatedDoublesCurve.from(MONTHS_CURVE, shiftedValues, INTERPOLATOR_EXPONENTIAL, NAME);
    PriceIndexInterpolatedCurve curveShiftedExpected =
        PriceIndexInterpolatedCurve.of(interpolatedShifted, VALUATION_MONTH);
    PriceIndexCurve curveShiftedComputed =
        PRICE_CURVE.shiftCurve(shifts);
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      double valueExpected = curveShiftedExpected.getPriceIndex(TEST_MONTHS[i]);
      double valueComputed = curveShiftedComputed.getPriceIndex(TEST_MONTHS[i]);
      assertEquals(valueExpected, valueComputed, TOLERANCE_VALUE);
    }
  }

  @Test
  public void test_shift_curve_wrong_length() {
    double[] shifts = new double[VALUES.length + 1];
    assertThrowsIllegalArg(() -> PRICE_CURVE.shiftCurve(shifts));
  }

  @Test
  public void test_curve_sensitivity() {
    double shift = 0.0001;
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      Double[] sensitivityComputed = PRICE_CURVE.getPriceIndexParameterSensitivity(TEST_MONTHS[i]);
      Double[] sensitivityExpected = new Double[VALUES.length];
      for (int j = 0; j < VALUES.length; j++) {
        double[] valueFd = new double[2];
        for (int k = 0; k < 2; k++) {
          double[] shifts = new double[VALUES.length];
          shifts[j] += (k == 0) ? -shift : shift;
          PriceIndexCurve curveShifted = PRICE_CURVE.shiftCurve(shifts);
          valueFd[k] = curveShifted.getPriceIndex(TEST_MONTHS[i]);
        }
        sensitivityExpected[j] = (valueFd[1] - valueFd[0]) / (2 * shift);
        assertEquals(sensitivityComputed[j], sensitivityExpected[j],
            TOLERANCE_DELTA, "Test: " + i + " - sensitivity: " + j);
      }
    }
  }

}
