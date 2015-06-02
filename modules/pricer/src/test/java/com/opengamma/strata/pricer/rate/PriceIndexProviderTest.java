/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.JP_CPI_EXF;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.interpolation.NaturalCubicSplineInterpolator1D;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.ForwardPriceIndexValues;
import com.opengamma.strata.market.value.PriceIndexValues;

/**
 * Test.
 */
@Test
public class PriceIndexProviderTest {

  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final YearMonth VAL_MONTH = YearMonth.of(2014, 6);

  private static final PriceIndexValues USCPIU_CURVE = ForwardPriceIndexValues.of(
      GB_RPI,
      VAL_MONTH,
      LocalDateDoubleTimeSeries.of(date(2013, 11, 30), 252),
      InterpolatedNodalCurve.of("GB-RPI", new double[] {1d, 10d}, new double[] {252d, 252d}, INTERPOLATOR));
  private static final PriceIndexValues GBPRI_CURVE = ForwardPriceIndexValues.of(
      GB_RPI,
      VAL_MONTH,
      LocalDateDoubleTimeSeries.of(date(2013, 11, 30), 252),
      InterpolatedNodalCurve.of("GB-RPI", new double[] {1d, 10d}, new double[] {252d, 252d}, INTERPOLATOR));
  private static final PriceIndexValues JPCPIEXF_CURVE = ForwardPriceIndexValues.of(
      GB_RPI,
      VAL_MONTH,
      LocalDateDoubleTimeSeries.of(date(2013, 11, 30), 194d),
      InterpolatedNodalCurve.of("GB-RPI", new double[] {1d, 10d}, new double[] {194d, 194d}, INTERPOLATOR));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(
        US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    PriceIndexProvider test = PriceIndexProvider.builder().priceIndexValues(map).build();
    assertEquals(test.getPriceIndexValues(), map);
  }

  public void test_of_index() {
    PriceIndexProvider test = PriceIndexProvider.of(GB_RPI, GBPRI_CURVE);
    ImmutableMap<PriceIndex, PriceIndexValues> expected = ImmutableMap.of(GB_RPI, GBPRI_CURVE);
    assertEquals(test.getPriceIndexValues(), expected);
  }

  public void test_of_map() {
    ImmutableMap<PriceIndex, PriceIndexValues> map = ImmutableMap.of(US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE);
    PriceIndexProvider test = PriceIndexProvider.of(map);
    assertEquals(test.getPriceIndexValues(), map);
  }

  //-------------------------------------------------------------------------
  public void test_parameterSensitivity_empty() {
    PointSensitivityBuilder pointSensi = PointSensitivityBuilder.none();
    PriceIndexProvider priceIndexProvider = PriceIndexProvider.empty();
    CurveParameterSensitivities computed = priceIndexProvider.parameterSensitivity(pointSensi.build());
    assertEquals(computed, CurveParameterSensitivities.empty());
  }

  public void test_parameterSensitivity() {
    double eps = 1.0e-13;
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    YearMonth valuationMonth = YearMonth.of(2014, 1);
    double[] x = new double[] {0.5, 1.0, 2.0};
    double[] y = new double[] {224.2, 262.6, 277.5};
    NaturalCubicSplineInterpolator1D interp = Interpolator1DFactory.NATURAL_CUBIC_SPLINE_INSTANCE;
    String curveName = "GB_RPI_CURVE";
    InterpolatedNodalCurve interpCurve = InterpolatedNodalCurve.of(curveName, x, y, interp);
    PriceIndexValues values = ForwardPriceIndexValues.of(
        GB_RPI,
        valuationMonth,
        LocalDateDoubleTimeSeries.of(date(2013, 11, 30), 200),
        interpCurve);
    PriceIndexProvider priceIndexProvider = PriceIndexProvider.of(GB_RPI, values);

    double pointSensiValue = 2.5;
    YearMonth refMonth = YearMonth.from(valuationDate.plusMonths(9));
    InflationRateSensitivity pointSensi = InflationRateSensitivity.of(GB_RPI, refMonth, pointSensiValue);
    CurveParameterSensitivities computed = priceIndexProvider.parameterSensitivity(pointSensi.build());
    double[] sensiComputed =
        computed.getSensitivities().get(NameCurrencySensitivityKey.of(curveName, pointSensi.getCurrency()));

    double[] sensiExpectedUnit =
        priceIndexProvider.getPriceIndexValues().get(GB_RPI).unitParameterSensitivity(refMonth);
    assertEquals(sensiComputed.length, sensiExpectedUnit.length);
    for (int i = 0; i < sensiComputed.length; ++i) {
      assertEquals(sensiComputed[i], sensiExpectedUnit[i] * pointSensiValue, eps);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableMap<PriceIndex, PriceIndexValues> map1 = ImmutableMap.of(US_CPI_U, USCPIU_CURVE);
    ImmutableMap<PriceIndex, PriceIndexValues> map2 = ImmutableMap.of(GB_RPI, GBPRI_CURVE);
    PriceIndexProvider test1 = PriceIndexProvider.builder().priceIndexValues(map1).build();
    coverImmutableBean(test1);
    PriceIndexProvider test2 = PriceIndexProvider.builder().priceIndexValues(map2).build();
    coverBeanEquals(test1, test2);
  }

}
