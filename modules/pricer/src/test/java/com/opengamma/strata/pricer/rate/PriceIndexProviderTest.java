/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.JP_CPI_EXF;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.PriceIndexCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.PriceIndexCurveSimple;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Test.
 */
@Test
public class PriceIndexProviderTest {

  private static final PriceIndexCurve USCPIU_CURVE = new PriceIndexCurveSimple(new ConstantDoublesCurve(252.0d));
  private static final PriceIndexCurve GBPRI_CURVE = new PriceIndexCurveSimple(new ConstantDoublesCurve(252.0d));
  private static final PriceIndexCurve JPCPIEXF_CURVE = new PriceIndexCurveSimple(new ConstantDoublesCurve(194.0d));
  private static final PriceIndexCurve USCPIU_PRICE_INDEX_CURVE =
      new PriceIndexCurveSimple(new ConstantDoublesCurve(252.0d));
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(
        US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    PriceIndexProvider test = PriceIndexProvider.builder().priceIndexCurves(map).build();
    assertEquals(test.getPriceIndexCurves(), map);
  }

  public void test_of_index() {
    PriceIndexProvider test = PriceIndexProvider.of(GB_RPI, GBPRI_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> expected = ImmutableMap.of(GB_RPI, GBPRI_CURVE);
    assertEquals(test.getPriceIndexCurves(), expected);
  }

  public void test_of_map() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE);
    PriceIndexProvider test = PriceIndexProvider.of(map);
    assertEquals(test.getPriceIndexCurves(), map);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_index() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE);
    PriceIndexProvider test = PriceIndexProvider.of(map);
    ImmutableMap<PriceIndex, PriceIndexCurve> expected = ImmutableMap.of(
        US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    test = test.combinedWith(JP_CPI_EXF, JPCPIEXF_CURVE);
    assertEquals(test.getPriceIndexCurves(), expected);
  }

  public void test_combinedWith_index_same() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE);
    PriceIndexProvider test = PriceIndexProvider.of(map);
    assertThrowsIllegalArg(() -> test.combinedWith(US_CPI_U, USCPIU_CURVE));
  }

  public void test_combinedWith_other() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map1 = ImmutableMap.of(US_CPI_U, USCPIU_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> map2 = ImmutableMap.of(GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    PriceIndexProvider test = PriceIndexProvider.empty();
    PriceIndexProvider other1 = PriceIndexProvider.of(map1);
    PriceIndexProvider other2 = PriceIndexProvider.of(map2);
    PriceIndexProvider other3 = PriceIndexProvider.empty();
    test = test.combinedWith(other1);
    test = test.combinedWith(other2);
    test = test.combinedWith(other3);
    ImmutableMap<PriceIndex, PriceIndexCurve> expected = ImmutableMap.of(
        US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    assertEquals(test.getPriceIndexCurves(), expected);
  }

  public void test_combinedWith_other_same() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map1 = ImmutableMap.of(GB_RPI, GBPRI_CURVE, US_CPI_U, USCPIU_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> map2 = ImmutableMap.of(GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    PriceIndexProvider test = PriceIndexProvider.of(map1);
    PriceIndexProvider other = PriceIndexProvider.of(map2);
    assertThrowsIllegalArg(() -> test.combinedWith(test.combinedWith(other)));
  }

  //-------------------------------------------------------------------------
  public void test_inflationIndexRate_historic() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_PRICE_INDEX_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexCurves(map).build();
    LocalDate prevMonthEnd = LocalDate.of(2014, 5, 31);
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(prevMonthEnd, 248.0d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(US_CPI_U, ts))
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .dayCount(ACT_ACT_ISDA)
        .build();
    double rate = test.data(PriceIndexProvider.class).inflationIndexRate(US_CPI_U, YearMonth.of(2014, 5), test);
    assertEquals(rate, 248.0d);
  }

  public void test_inflationIndexRate_forward() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_PRICE_INDEX_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexCurves(map).build();
    LocalDate prevMonthEnd = LocalDate.of(2014, 5, 31);
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(prevMonthEnd, 0.06);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(US_CPI_U, ts))
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .dayCount(ACT_ACT_ISDA)
        .build();
    double rate = test.data(PriceIndexProvider.class).inflationIndexRate(US_CPI_U, YearMonth.of(2015, 6), test);
    assertEquals(rate, 252.0d);
  }

  public void inflationIndexRateSensitivity_none() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_PRICE_INDEX_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexCurves(map).build();
    LocalDate prevMonthEnd = LocalDate.of(2014, 5, 31);
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(prevMonthEnd, 0.06);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(US_CPI_U, ts))
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .dayCount(ACT_ACT_ISDA)
        .build();
    YearMonth referenceMonth = YearMonth.of(2014, 5);
    PointSensitivityBuilder sensiComputed =
        test.data(PriceIndexProvider.class).inflationIndexRateSensitivity(US_CPI_U, referenceMonth, test);
    assertEquals(sensiComputed, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void inflationIndexRateSensitivity() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_PRICE_INDEX_CURVE);
    PriceIndexProvider priceIndexMap = PriceIndexProvider.builder().priceIndexCurves(map).build();
    LocalDate prevMonthEnd = LocalDate.of(2014, 5, 31);
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(prevMonthEnd, 0.06);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(US_CPI_U, ts))
        .additionalData(ImmutableMap.of(priceIndexMap.getClass(), priceIndexMap))
        .dayCount(ACT_ACT_ISDA)
        .build();
    YearMonth referenceMonth = YearMonth.of(2016, 6);
    PointSensitivityBuilder sensiComputed =
        test.data(PriceIndexProvider.class).inflationIndexRateSensitivity(US_CPI_U, referenceMonth, test);
    InflationRateSensitivity sensiExpected = InflationRateSensitivity.of(US_CPI_U, referenceMonth, 1.0d);
    assertEquals(sensiComputed, sensiExpected);
  }

  //-------------------------------------------------------------------------
  public void test_parameterSensitivity_empty() {
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    PointSensitivityBuilder pointSensi = PointSensitivityBuilder.none();
    PriceIndexProvider priceIndexProvider = PriceIndexProvider.empty();
    RatesProvider ratesProvider = ImmutableRatesProvider.builder()
        .valuationDate(valuationDate)
        .dayCount(ACT_ACT_ISDA)
        .build();
    CurveParameterSensitivity computed = priceIndexProvider.parameterSensitivity(pointSensi.build(), ratesProvider);
    assertEquals(computed, CurveParameterSensitivity.empty());
  }

  public void test_parameterSensitivity() {
    double eps = 1.0e-13;
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    double[] x = new double[] {0.5, 1.0, 2.0};
    double[] y = new double[] {224.2, 262.6, 277.5};
    CombinedInterpolatorExtrapolator interp =
        CombinedInterpolatorExtrapolatorFactory.getInterpolator(
            Interpolator1DFactory.NATURAL_CUBIC_SPLINE,
            Interpolator1DFactory.FLAT_EXTRAPOLATOR,
            Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    String curveName = "GB_RPI_CURVE";
    InterpolatedDoublesCurve interpCurve = InterpolatedDoublesCurve.from(x, y, interp, curveName);
    PriceIndexCurveSimple curve = new PriceIndexCurveSimple(interpCurve);
    PriceIndexProvider priceIndexProvider = PriceIndexProvider.of(GB_RPI, curve);
    RatesProvider ratesProvider = ImmutableRatesProvider.builder()
        .valuationDate(valuationDate)
        .timeSeries(ImmutableMap.of(US_CPI_U, LocalDateDoubleTimeSeries.empty()))
        .dayCount(ACT_ACT_ISDA)
        .build();

    double pointSensiValue = 2.5;
    YearMonth refMonth = YearMonth.from(valuationDate.plusMonths(9));
    InflationRateSensitivity pointSensi = InflationRateSensitivity.of(GB_RPI, refMonth, pointSensiValue);
    CurveParameterSensitivity computed = priceIndexProvider.parameterSensitivity(pointSensi.build(), ratesProvider);
    double[] sensiComputed =
        computed.getSensitivities().get(NameCurrencySensitivityKey.of(curveName, pointSensi.getCurrency()));

    double time = ratesProvider.relativeTime(refMonth.atEndOfMonth());
    double[] sensiExpectedUnit =
        priceIndexProvider.getPriceIndexCurves().get(GB_RPI).getPriceIndexParameterSensitivity(time);
    assertEquals(sensiComputed.length, sensiExpectedUnit.length);
    for (int i = 0; i < sensiComputed.length; ++i) {
      assertEquals(sensiComputed[i], sensiExpectedUnit[i] * pointSensiValue, eps);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map1 = ImmutableMap.of(US_CPI_U, USCPIU_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> map2 = ImmutableMap.of(GB_RPI, GBPRI_CURVE);
    PriceIndexProvider test1 = PriceIndexProvider.builder().priceIndexCurves(map1).build();
    coverImmutableBean(test1);
    PriceIndexProvider test2 = PriceIndexProvider.builder().priceIndexCurves(map2).build();
    coverBeanEquals(test1, test2);
  }

}
