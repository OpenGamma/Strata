/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.time.temporal.ChronoUnit.MONTHS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.InflationNodalCurve;
import com.opengamma.strata.market.curve.SeasonalityDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.UnitParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Tests {@link SimplePriceIndexValues}.
 */
@Test
public class SimplePriceIndexValuesTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2015, 5, 3);
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

  private static final DoubleArray TIMES = DoubleArray.of(9.0, 21.0, 57.0, 117.0);
  private static final DoubleArray VALUES = DoubleArray.of(240.500, 245.000, 265.000, 286.000);
  private static final DoubleArray VALUES2 = DoubleArray.of(243.500, 248.000, 268.000, 289.000);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("USD-HICP");
  private static final CurveMetadata METADATA = Curves.prices(NAME);
  private static final InterpolatedNodalCurve CURVE_NOFIX = InterpolatedNodalCurve.of(METADATA, TIMES, VALUES, INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2_NOFIX = InterpolatedNodalCurve.of(METADATA, TIMES, VALUES2, INTERPOLATOR);
  private static final DoubleArray SEASONALITY_MULTIPLICATIVE = DoubleArray.of(
      1.002754153722096, 1.001058905136103, 1.006398754528882, 1.000862459308375,
      0.998885402944655, 0.995571243121412, 1.001419845026233, 1.001663068058397,
      0.999147014890734, 0.998377467899150, 0.999570726482709, 0.994346721844999);
  private static final SimplePriceIndexValues INSTANCE =
      SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);

  private static final YearMonth[] TEST_MONTHS = new YearMonth[] {
      YearMonth.of(2015, 1), YearMonth.of(2015, 5), YearMonth.of(2016, 5), YearMonth.of(2016, 6), YearMonth.of(2024, 12)};
  private static final PriceIndexObservation[] TEST_OBS = new PriceIndexObservation[] {
      PriceIndexObservation.of(US_CPI_U, YearMonth.of(2015, 1)),
      PriceIndexObservation.of(US_CPI_U, YearMonth.of(2015, 5)),
      PriceIndexObservation.of(US_CPI_U, YearMonth.of(2016, 5)),
      PriceIndexObservation.of(US_CPI_U, YearMonth.of(2016, 6)),
      PriceIndexObservation.of(US_CPI_U, YearMonth.of(2024, 12))};
  private static final double TOLERANCE_VALUE = 1.0E-10;
  private static final double TOLERANCE_DELTA = 1.0E-6;

  //-------------------------------------------------------------------------
  @SuppressWarnings("deprecation")
  public void test_NO_SEASONALITY() {
    assertEquals(SimplePriceIndexValues.NO_SEASONALITY, DoubleArray.filled(12, 1d));
  }

  @SuppressWarnings("deprecation")
  public void test_of_noSeasonality() {
    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);
    assertEquals(test.getIndex(), US_CPI_U);
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.getSeasonality(), DoubleArray.filled(12, 1d));
    assertEquals(test.getCurve(), CURVE_NOFIX);
    assertEquals(test.getParameterCount(), CURVE_NOFIX.getParameterCount());
    assertEquals(test.getParameter(0), CURVE_NOFIX.getParameter(0));
    assertEquals(test.getParameterMetadata(0), CURVE_NOFIX.getParameterMetadata(0));
    assertEquals(test.withParameter(0, 1d).getCurve(), CURVE_NOFIX.withParameter(0, 1d));
    assertEquals(test.withPerturbation((i, v, m) -> v + 1d).getCurve(), CURVE_NOFIX.withPerturbation((i, v, m) -> v + 1d));
    assertEquals(test.findData(CURVE_NOFIX.getName()), Optional.of(CURVE_NOFIX));
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
    // check PriceIndexValues
    PriceIndexValues test2 = PriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);
    assertEquals(test, test2);
  }

  @SuppressWarnings("deprecation")
  public void test_of_seasonality() {
    SimplePriceIndexValues test =
        SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS, SEASONALITY_MULTIPLICATIVE);
    assertEquals(test.getIndex(), US_CPI_U);
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.getSeasonality(), SEASONALITY_MULTIPLICATIVE);
    YearMonth lastMonth = YearMonth.from(USCPI_TS.getLatestDate());
    double nbMonth = YearMonth.from(VAL_DATE).until(lastMonth, MONTHS);
    InflationNodalCurve seasonalCurve = InflationNodalCurve.of(
        CURVE_NOFIX, VAL_DATE, lastMonth, nbMonth, SeasonalityDefinition.of(SEASONALITY_MULTIPLICATIVE, ShiftType.SCALED));
    assertEquals(test.getCurve(), seasonalCurve);
  }

  @SuppressWarnings("deprecation")
  public void test_of_wrongSeasonalityLength() {
    assertThrowsIllegalArg(() -> SimplePriceIndexValues.of(
        US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS, DoubleArray.EMPTY));
  }

  @SuppressWarnings("deprecation")
  public void test_of_startDateBeforeFixing() {
    DoubleArray monthWrong = DoubleArray.of(-10.0, 21.0, 57.0, 117.0);
    InterpolatedNodalCurve interpolated = CURVE_NOFIX.toBuilder().xValues(monthWrong).build();
    assertThrowsIllegalArg(() -> SimplePriceIndexValues.of(
        US_CPI_U, VAL_DATE, interpolated, USCPI_TS, SEASONALITY_MULTIPLICATIVE));
  }

  //-------------------------------------------------------------------------
  public void test_valuePointSensitivity_fixing() {
    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);
    PriceIndexObservation obs = PriceIndexObservation.of(US_CPI_U, VAL_MONTH.minusMonths(3));
    assertEquals(test.valuePointSensitivity(obs), PointSensitivityBuilder.none());
  }

  public void test_valuePointSensitivity_forward() {
    YearMonth month = VAL_MONTH.plusMonths(3);
    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);
    PriceIndexObservation obs = PriceIndexObservation.of(US_CPI_U, month);
    InflationRateSensitivity expected = InflationRateSensitivity.of(obs, 1d);
    assertEquals(test.valuePointSensitivity(obs), expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);
    InflationRateSensitivity point =
        InflationRateSensitivity.of(PriceIndexObservation.of(US_CPI_U, VAL_MONTH.plusMonths(3)), 1d);
    assertEquals(test.parameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void test_createParameterSensitivity() {
    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15, 0.16, 0.17);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertEquals(sens.getSensitivities().get(0), CURVE_NOFIX.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  public void test_withCurve() {
    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS).withCurve(CURVE2_NOFIX);
    assertEquals(test.getCurve(), CURVE2_NOFIX);
  }

  //-------------------------------------------------------------------------
  public void test_parameter_count() {
    assertEquals(INSTANCE.getParameterCount(), CURVE_NOFIX.getParameterCount());
  }

  public void test_parameter() {
    assertEquals(INSTANCE.getParameter(2), CURVE_NOFIX.getParameter(2));
  }

  public void test_parameter_metadata() {
    assertEquals(INSTANCE.getParameterMetadata(2), CURVE_NOFIX.getParameterMetadata(2));
  }

  //-------------------------------------------------------------------------
  public void test_value() {
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      double valueComputed = INSTANCE.value(TEST_OBS[i]);
      YearMonth fixingMonth = TEST_OBS[i].getFixingMonth();
      double valueExpected;
      if (USCPI_TS.containsDate(fixingMonth.atEndOfMonth())) {
        valueExpected = USCPI_TS.get(fixingMonth.atEndOfMonth()).getAsDouble();
      } else {
        double x = YearMonth.from(VAL_DATE).until(fixingMonth, MONTHS);
        valueExpected = CURVE_NOFIX.yValue(x);
      }
      assertEquals(valueComputed, valueExpected, TOLERANCE_VALUE, "test " + i);
    }
  }

  public void test_value_pts_sensitivity() {
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      PointSensitivityBuilder ptsComputed = INSTANCE.valuePointSensitivity(TEST_OBS[i]);
      YearMonth fixingMonth = TEST_OBS[i].getFixingMonth();
      PointSensitivityBuilder ptsExpected;
      if (USCPI_TS.containsDate(fixingMonth.atEndOfMonth())) {
        ptsExpected = PointSensitivityBuilder.none();
      } else {
        ptsExpected = InflationRateSensitivity.of(TEST_OBS[i], 1d);
      }
      assertTrue(ptsComputed.build().equalWithTolerance(ptsExpected.build(), TOLERANCE_VALUE), "test " + i);
    }
  }

  public void test_value_parameter_sensitivity() {
    for (int i = 0; i < TEST_MONTHS.length; i++) {
      YearMonth fixingMonth = TEST_OBS[i].getFixingMonth();
      if (!USCPI_TS.containsDate(fixingMonth.atEndOfMonth())) {
        InflationRateSensitivity ptsExpected = (InflationRateSensitivity) InflationRateSensitivity.of(TEST_OBS[i], 1d);
        CurrencyParameterSensitivities psComputed = INSTANCE.parameterSensitivity(ptsExpected);
        double x = YearMonth.from(VAL_DATE).until(fixingMonth, MONTHS);
        UnitParameterSensitivities sens1 = UnitParameterSensitivities.of(CURVE_NOFIX.yValueParameterSensitivity(x));
        CurrencyParameterSensitivities psExpected =
            sens1.multipliedBy(ptsExpected.getCurrency(), ptsExpected.getSensitivity());
        assertTrue(psComputed.equalWithTolerance(psExpected, TOLERANCE_DELTA), "test " + i);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimplePriceIndexValues instance1 = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE_NOFIX, USCPI_TS);
    coverImmutableBean(instance1);
    SimplePriceIndexValues test2 =
        SimplePriceIndexValues.of(
            GB_HICP,
            VAL_DATE.plusMonths(1),
            CURVE_NOFIX,
            LocalDateDoubleTimeSeries.of(VAL_MONTH.minusMonths(2).atEndOfMonth(), 100d));
    coverBeanEquals(instance1, test2);
  }

}
