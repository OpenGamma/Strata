/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.time.temporal.ChronoUnit.MONTHS;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;

/**
 * Tests {@link ForwardPriceIndexValues}.
 */
@Test
public class ForwardPriceIndexValuesTest {

  private static final YearMonth VAL_MONTH = YearMonth.of(2015, 5);
  // USD HICP, CPURNSA Index
  private static final double[] USCPI_VALUES = new double[] {
      211.143, 212.193, 212.709, 213.240, 213.856, 215.693, 215.351, 215.834, 215.969, 216.177, 216.330, 215.949, // 2009
      216.687, 216.741, 217.631, 218.009, 218.178, 217.965, 218.011, 218.312, 218.439, 218.711, 218.803, 219.179, // 2010
      220.223, 221.309, 223.467, 224.906, 225.964, 225.722, 225.922, 226.545, 226.889, 226.421, 226.230, 225.672, // 2011
      226.655, 227.663, 229.392, 230.085, 229.815, 229.478, 229.104, 230.379, 231.407, 231.317, 230.221, 229.601, // 2012
      230.280, 232.166, 232.773, 232.531, 232.945, 233.504, 233.596, 233.877, 234.149, 233.546, 233.069, 233.049, // 2013
      233.916, 234.781, 236.293, 237.072, 237.900, 238.343, 238.250, 237.852, 238.031, 237.433, 236.151, 234.812, // 2014
      233.707, 234.722, 236.119}; // 2015
  private static final LocalDate USCPI_START_DATE = LocalDate.of(2009, 1, 31);
  private static final LocalDateDoubleTimeSeries USCPI_TS;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < USCPI_VALUES.length; i++) {
      builder.put(USCPI_START_DATE.plusMonths(i), USCPI_VALUES[i]);
    }
    USCPI_TS = builder.build();
  }

  private static final double[] TIMES = new double[] {9.0, 21.0, 57.0, 117.0};
  private static final double[] VALUES = new double[] {240.500, 245.000, 265.000, 286.000};
  private static final double[] VALUES2 = new double[] {243.500, 248.000, 268.000, 289.000};
  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final CurveName NAME = CurveName.of("USD-HICP");
  private static final CurveMetadata METADATA = Curves.prices(NAME);
  private static final InterpolatedNodalCurve CURVE = InterpolatedNodalCurve.of(METADATA, TIMES, VALUES, INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 = InterpolatedNodalCurve.of(METADATA, TIMES, VALUES2, INTERPOLATOR);
  private static final List<Double> SEASONALITY = ImmutableList.copyOf(
      new Double[] {0.98d, 0.99d, 1.01d, 1.00d, 1.00d, 1.01d, 1.01d, 0.99d, 1.00d, 1.00d, 1.00d, 1.01d});
  private static final ForwardPriceIndexValues INSTANCE =
      ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, CURVE, SEASONALITY);

  private static final YearMonth[] TEST_MONTHS = new YearMonth[] {
      YearMonth.of(2015, 1), YearMonth.of(2015, 5), YearMonth.of(2016, 5), YearMonth.of(2016, 6), YearMonth.of(2024, 12)};
  private static final double TOLERANCE_VALUE = 1.0E-10;
  private static final double TOLERANCE_DELTA = 1.0E-5;

  //-------------------------------------------------------------------------
  public void test_NO_SEASONALITY() {
    assertEquals(ForwardPriceIndexValues.NO_SEASONALITY, Collections.nCopies(12, 1d));
  }

  public void test_of_noSeasonality() {
    ForwardPriceIndexValues test = ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, CURVE);
    assertEquals(test.getIndex(), US_CPI_U);
    assertEquals(test.getValuationMonth(), VAL_MONTH);
    assertEquals(test.getSeasonality(), Collections.nCopies(12, 1d));
    assertEquals(test.getCurve(), CURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), TIMES.length);
  }

  public void test_of_seasonality() {
    ForwardPriceIndexValues test = ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, CURVE, SEASONALITY);
    assertEquals(test.getIndex(), US_CPI_U);
    assertEquals(test.getValuationMonth(), VAL_MONTH);
    assertEquals(test.getSeasonality(), SEASONALITY);
    assertEquals(test.getCurve(), CURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), TIMES.length);
  }

  public void test_of_wrongSeasonalityLength() {
    assertThrowsIllegalArg(() -> ForwardPriceIndexValues.of(
        US_CPI_U, VAL_MONTH, USCPI_TS, CURVE, new ArrayList<>()));
  }

  public void test_of_startDateBeforeFixing() {
    double[] monthWrong = new double[] {-10.0, 21.0, 57.0, 117.0};
    InterpolatedNodalCurve interpolated = CURVE.toBuilder().xValues(monthWrong).build();
    assertThrowsIllegalArg(() -> ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, interpolated, SEASONALITY));
  }

  //-------------------------------------------------------------------------
  public void test_value() {
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      YearMonth lastMonth = YearMonth.from(USCPI_TS.getLatestDate());
      double nbMonthLast = VAL_MONTH.until(lastMonth, MONTHS);
      InterpolatedNodalCurve finalCurve = CURVE.withNode(0, nbMonthLast, USCPI_TS.getLatestValue());
      double nbMonth = VAL_MONTH.until(TEST_MONTHS[i], MONTHS);
      OptionalDouble valueTs = USCPI_TS.get(TEST_MONTHS[i].atEndOfMonth());
      double adj = SEASONALITY.get(TEST_MONTHS[i].getMonthValue() - 1);
      double valueExpected = valueTs.isPresent() ? valueTs.getAsDouble() : finalCurve.yValue(nbMonth) * adj;
      double valueComputed = INSTANCE.value(TEST_MONTHS[i]);
      assertEquals(valueExpected, valueComputed, TOLERANCE_VALUE);
    }
  }

  //-------------------------------------------------------------------------
  public void test_valuePointSensitivity_fixing() {
    ForwardPriceIndexValues test = ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, CURVE);
    assertEquals(test.valuePointSensitivity(VAL_MONTH.minusMonths(3)), PointSensitivityBuilder.none());
  }

  public void test_valuePointSensitivity_forward() {
    YearMonth month = VAL_MONTH.plusMonths(3);
    ForwardPriceIndexValues test = ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, CURVE);
    InflationRateSensitivity expected = InflationRateSensitivity.of(US_CPI_U, month, 1d);
    assertEquals(test.valuePointSensitivity(month), expected);
  }

  //-------------------------------------------------------------------------
  public void test_unitParameterSensitivity() {
    double shift = 0.0001;
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      double[] sensitivityComputed =
          INSTANCE.unitParameterSensitivity(TEST_MONTHS[i]).getSensitivity(NAME).getSensitivity();
      double[] sensitivityExpected = new double[VALUES.length];
      for (int j = 0; j < VALUES.length; j++) {
        double[] valueFd = new double[2];
        for (int k = 0; k < 2; k++) {
          List<ValueAdjustment> adjustments = new ArrayList<>();
          for (int l = 0; l < VALUES.length; l++) {
            adjustments.add(ValueAdjustment.ofDeltaAmount((l == j) ? ((k == 0) ? -shift : shift) : 0.0d));
          }
          ForwardPriceIndexValues curveShifted = INSTANCE.withCurve(INSTANCE.getCurve().shiftedBy(adjustments));
          valueFd[k] = curveShifted.value(TEST_MONTHS[i]);
        }
        sensitivityExpected[j] = (valueFd[1] - valueFd[0]) / (2 * shift);
        assertEquals(sensitivityComputed[j], sensitivityExpected[j], TOLERANCE_DELTA, "Test: " + i + " - sensi: " + j);
      }
    }
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_curveParameterSensitivity() {
    ForwardPriceIndexValues test = ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, CURVE);
    InflationRateSensitivity point = InflationRateSensitivity.of(US_CPI_U, VAL_MONTH.plusMonths(3), 1d);
    assertEquals(test.curveParameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void test_withCurve() {
    ForwardPriceIndexValues test = ForwardPriceIndexValues.of(US_CPI_U, VAL_MONTH, USCPI_TS, CURVE).withCurve(CURVE2);
    assertEquals(test.getCurve(), CURVE2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(INSTANCE);
    ForwardPriceIndexValues test2 =
        ForwardPriceIndexValues.of(
            GB_HICP,
            VAL_MONTH.plusMonths(1),
            LocalDateDoubleTimeSeries.of(VAL_MONTH.minusMonths(2).atEndOfMonth(), 100d),
            CURVE);
    coverBeanEquals(INSTANCE, test2);
  }

}
