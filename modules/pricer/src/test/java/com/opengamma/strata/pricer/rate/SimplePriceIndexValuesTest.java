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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
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
  private static final DoubleArray SEASONALITY_ADDITIVE = DoubleArray.of(
      1.0, 1.5, 1.0, -0.5,
      -0.5, -1.0, -1.5, 0.0,
      0.5, 1.0, 1.0, -2.5);
//  private static final SimplePriceIndexValues INSTANCE =
//      SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);
//  private static final SimplePriceIndexValues INSTANCE_2 =
//      SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);

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
//  public void test_NO_SEASONALITY() {
//    assertEquals(SimplePriceIndexValues.NO_SEASONALITY, DoubleArray.filled(12, 1d));
//  }
//
//  public void test_of_noSeasonality() {
//    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);
//    assertEquals(test.getIndex(), US_CPI_U);
//    assertEquals(test.getValuationDate(), VAL_DATE);
//    assertEquals(test.getSeasonality(), DoubleArray.filled(12, 1d));
//    assertEquals(test.getCurve(), CURVE);
//    assertEquals(test.getParameterCount(), CURVE.getParameterCount());
//    assertEquals(test.getParameter(0), CURVE.getParameter(0));
//    assertEquals(test.getParameterMetadata(0), CURVE.getParameterMetadata(0));
//    assertEquals(test.withParameter(0, 1d).getCurve(), CURVE.withParameter(0, 1d));
//    assertEquals(test.withPerturbation((i, v, m) -> v + 1d).getCurve(), CURVE.withPerturbation((i, v, m) -> v + 1d));
//    assertEquals(test.findData(CURVE.getName()), Optional.of(CURVE));
//    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
//    // check PriceIndexValues
//    PriceIndexValues test2 = PriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);
//    assertEquals(test, test2);
//  }
//
//  public void test_of_seasonality() {
//    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS, SEASONALITY_MULTIPLICATIVE);
//    assertEquals(test.getIndex(), US_CPI_U);
//    assertEquals(test.getValuationDate(), VAL_DATE);
//    assertEquals(test.getSeasonality(), SEASONALITY_MULTIPLICATIVE);
//    assertEquals(test.getCurve(), CURVE);
//  }
//
//  public void test_of_wrongSeasonalityLength() {
//    assertThrowsIllegalArg(() -> SimplePriceIndexValues.of(
//        US_CPI_U, VAL_DATE, CURVE, USCPI_TS, DoubleArray.EMPTY));
//  }
//
//  public void test_of_startDateBeforeFixing() {
//    DoubleArray monthWrong = DoubleArray.of(-10.0, 21.0, 57.0, 117.0);
//    InterpolatedNodalCurve interpolated = CURVE.toBuilder().xValues(monthWrong).build();
//    assertThrowsIllegalArg(() -> SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, interpolated, USCPI_TS, SEASONALITY_MULTIPLICATIVE));
//  }
//
//  //-------------------------------------------------------------------------
//  public void test_value_multiplicative() {
//    for (int i = 1; i < TEST_MONTHS.length; i++) {
//      YearMonth lastMonth = YearMonth.from(USCPI_TS.getLatestDate());
//      double nbMonthLast = VAL_MONTH.until(lastMonth, MONTHS); // March 15 - May 15 = -2
//      int nbMonthFromLast = (int) (-nbMonthLast);
//      InterpolatedNodalCurve finalCurve = CURVE.withNode(nbMonthLast, USCPI_TS.getLatestValue(), ParameterMetadata.empty());
//      double nbMonth = VAL_MONTH.until(TEST_MONTHS[i], MONTHS);
//      OptionalDouble valueTs = USCPI_TS.get(TEST_MONTHS[i].atEndOfMonth());
//      double[] adjComp = new double[12];
//      adjComp[nbMonthFromLast] = 1;
//      for (int j = nbMonthFromLast + 1; j < nbMonthFromLast + 12; j++) {
//        adjComp[j % 12] = adjComp[(j - 1) % 12] * SEASONALITY_MULTIPLICATIVE.get(j % 12);
//      }
//      double adj = adjComp[TEST_MONTHS[i].getMonthValue() - 1];
//      double valueExpected = valueTs.isPresent() ? valueTs.getAsDouble() : finalCurve.yValue(nbMonth) * adj;
//      double valueComputed = INSTANCE_MULTIPLICATIVE.value(TEST_OBS[i]);
//      assertEquals(valueExpected, valueComputed, TOLERANCE_VALUE, "month " + i);
//    }
//  }
//  
//  public void test_value_additive() {
//    for (int i = 1; i < TEST_MONTHS.length; i++) {
//      YearMonth lastMonth = YearMonth.from(USCPI_TS.getLatestDate());
//      double nbMonthLast = VAL_MONTH.until(lastMonth, MONTHS); // March 15 - May 15 = -2
//      int nbMonthFromLast = (int) (-nbMonthLast);
//      InterpolatedNodalCurve finalCurve = CURVE.withNode(nbMonthLast, USCPI_TS.getLatestValue(), ParameterMetadata.empty());
//      double nbMonth = VAL_MONTH.until(TEST_MONTHS[i], MONTHS);
//      OptionalDouble valueTs = USCPI_TS.get(TEST_MONTHS[i].atEndOfMonth());
//      double[] adjComp = new double[12];
//      adjComp[nbMonthFromLast] = 0;
//      for (int j = nbMonthFromLast + 1; j < nbMonthFromLast + 12; j++) {
//        adjComp[j % 12] = adjComp[(j - 1) % 12] + SEASONALITY_ADDITIVE.get(j % 12);
//      }
//      double adj = adjComp[TEST_MONTHS[i].getMonthValue() - 1];
//      double valueExpected = valueTs.isPresent() ? valueTs.getAsDouble() : finalCurve.yValue(nbMonth) + adj;
//      double valueComputed = INSTANCE_ADDITIVE.value(TEST_OBS[i]);
//      assertEquals(valueExpected, valueComputed, TOLERANCE_VALUE, "month " + i);
//    }
//  }
//
//  //-------------------------------------------------------------------------
//  public void test_valuePointSensitivity_fixing() {
//    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);
//    PriceIndexObservation obs = PriceIndexObservation.of(US_CPI_U, VAL_MONTH.minusMonths(3));
//    assertEquals(test.valuePointSensitivity(obs), PointSensitivityBuilder.none());
//  }
//
//  public void test_valuePointSensitivity_forward() {
//    YearMonth month = VAL_MONTH.plusMonths(3);
//    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);
//    PriceIndexObservation obs = PriceIndexObservation.of(US_CPI_U, month);
//    InflationRateSensitivity expected = InflationRateSensitivity.of(obs, 1d);
//    assertEquals(test.valuePointSensitivity(obs), expected);
//  }
//
//  //-------------------------------------------------------------------------
//  public void test_unitParameterSensitivity_multiplicative() {
//    double shift = 1.0E-4;
//    for (int i = 0; i < TEST_MONTHS.length; i++) {
//      CurrencyParameterSensitivities cps =
//          INSTANCE_MULTIPLICATIVE.parameterSensitivity(InflationRateSensitivity.of(TEST_OBS[i], 1));
//      DoubleArray sensitivityComputed = cps.findSensitivity(NAME, Currency.USD)
//          .map(s -> s.getSensitivity())
//          .orElse(DoubleArray.filled(VALUES.size()));
//      for (int j = 0; j < VALUES.size(); j++) {
//        double[] valueFd = new double[2];
//        for (int k = 0; k < 2; k++) {
//          // copy indices to provide access in lambda
//          int jIndex = j;
//          int kIndex = k;
//          NodalCurve bumpedCurve = INSTANCE_MULTIPLICATIVE.getCurve()
//              .withPerturbation((idx, value, meta) -> (idx == jIndex) ? (kIndex == 0 ? -shift : shift) : 0d);
//          SimplePriceIndexValues curveShifted = INSTANCE_MULTIPLICATIVE.withCurve(bumpedCurve);
//          valueFd[k] = curveShifted.value(TEST_OBS[i]);
//        }
//        double sensitivityExpected = (valueFd[1] - valueFd[0]) / (2 * shift);
//        assertEquals(sensitivityComputed.get(j), sensitivityExpected, TOLERANCE_DELTA, "Test: " + i + " - sensi: " + j);
//      }
//    }
//  }
//
//  public void test_unitParameterSensitivity_additive() {
//    double shift = 1.0E-4;
//    for (int i = 0; i < TEST_MONTHS.length; i++) {
//      CurrencyParameterSensitivities cps =
//          INSTANCE_ADDITIVE.parameterSensitivity(InflationRateSensitivity.of(TEST_OBS[i], 1));
//      DoubleArray sensitivityComputed = cps.findSensitivity(NAME, Currency.USD)
//          .map(s -> s.getSensitivity())
//          .orElse(DoubleArray.filled(VALUES.size()));
//      for (int j = 0; j < VALUES.size(); j++) {
//        double[] valueFd = new double[2];
//        for (int k = 0; k < 2; k++) {
//          // copy indices to provide access in lambda
//          int jIndex = j;
//          int kIndex = k;
//          NodalCurve bumpedCurve = INSTANCE_ADDITIVE.getCurve()
//              .withPerturbation((idx, value, meta) -> (idx == jIndex) ? (kIndex == 0 ? -shift : shift) : 0d);
//          SimplePriceIndexValues curveShifted = INSTANCE_ADDITIVE.withCurve(bumpedCurve);
//          valueFd[k] = curveShifted.value(TEST_OBS[i]);
//        }
//        double sensitivityExpected = (valueFd[1] - valueFd[0]) / (2 * shift);
//        assertEquals(sensitivityComputed.get(j), sensitivityExpected, TOLERANCE_DELTA, "Test: " + i + " - sensi: " + j);
//      }
//    }
//  }
//
//  //-------------------------------------------------------------------------
//  // proper end-to-end tests are elsewhere
//  public void test_parameterSensitivity() {
//    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);
//    InflationRateSensitivity point =
//        InflationRateSensitivity.of(PriceIndexObservation.of(US_CPI_U, VAL_MONTH.plusMonths(3)), 1d);
//    assertEquals(test.parameterSensitivity(point).size(), 1);
//  }
//
//  //-------------------------------------------------------------------------
//  public void test_createParameterSensitivity() {
//    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS);
//    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15, 0.16, 0.17);
//    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
//    assertEquals(sens.getSensitivities().get(0), CURVE.createParameterSensitivity(USD, sensitivities));
//  }
//
//  //-------------------------------------------------------------------------
//  public void test_withCurve() {
//    SimplePriceIndexValues test = SimplePriceIndexValues.of(US_CPI_U, VAL_DATE, CURVE, USCPI_TS).withCurve(CURVE2);
//    assertEquals(test.getCurve(), CURVE2);
//  }
//
//  //-------------------------------------------------------------------------
//  public void coverage() {
//    coverImmutableBean(INSTANCE_MULTIPLICATIVE);
//    SimplePriceIndexValues test2 =
//        SimplePriceIndexValues.of(
//            GB_HICP,
//            VAL_DATE.plusMonths(1),
//            CURVE,
//            LocalDateDoubleTimeSeries.of(VAL_MONTH.minusMonths(2).atEndOfMonth(), 100d));
//    coverBeanEquals(INSTANCE_MULTIPLICATIVE, test2);
//  }

}
