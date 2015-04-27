/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.DiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test {@link ImmutableRatesProvider}.
 */
@Test
public class ImmutableRatesProviderTest {

  private static final LocalDate PREV2_DATE = LocalDate.of(2014, 6, 26);
  private static final LocalDate PREV_DATE = LocalDate.of(2014, 6, 27);
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final LocalDate NEXT_DATE = LocalDate.of(2014, 7, 1);

  private static final FxMatrix FX_MATRIX = FxMatrix.builder().addRate(GBP, USD, 1.6d).build();
  private static final DiscountCurve DISCOUNT_CURVE_GBP =
      new DiscountCurve("GBP-Discount", new ConstantDoublesCurve(0.99d));
  private static final DiscountCurve DISCOUNT_CURVE_USD =
      new DiscountCurve("USD-Discount", new ConstantDoublesCurve(0.95d));
  private static final YieldAndDiscountCurve USD_LIBOR_CURVE =
      new DiscountCurve("USD-LIBOR-3M", new ConstantDoublesCurve(0.97d)) {
        @Override
        public double getSimplyCompoundForwardRate(double startTime, double endTime, double accrualFactor) {
          return 0.0123d;
        }
      };
  private static final YieldAndDiscountCurve FED_FUND_CURVE =
      new DiscountCurve("USD-FED-FUND", new ConstantDoublesCurve(0.97d)) {
        @Override
        public double getSimplyCompoundForwardRate(double startTime, double endTime, double accrualFactor) {
          return 0.0123d;
        }
      };

  //-------------------------------------------------------------------------
  public void test_builder() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(ImmutableRatesProvider.meta().timeSeries().get(test), ImmutableMap.of(WM_GBP_USD, ts));
    assertEquals(ImmutableRatesProvider.meta().dayCount().get(test), ACT_ACT_ISDA);
  }

  //-------------------------------------------------------------------------
  public void test_timeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(date(2014, 6, 30), 3.2d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.timeSeries(IborIndices.USD_LIBOR_3M), ts);
    assertThrowsIllegalArg(() -> test.timeSeries(IborIndices.CHF_LIBOR_1W));
  }

  //-------------------------------------------------------------------------
  public void test_discountFactor() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.discountFactor(GBP, LocalDate.of(2014, 7, 30)), 0.99d, 0d);
  }

  public void test_discountFactor_notKnown() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrowsIllegalArg(() -> test.discountFactor(GBP, LocalDate.of(2014, 7, 30)));
  }

  //-------------------------------------------------------------------------
  public void test_discountFactorZeroRateSensitivity() {
    double relativeTime = ACT_ACT_ISDA.yearFraction(VAL_DATE, LocalDate.of(2014, 7, 30));
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP))
        .dayCount(ACT_ACT_ISDA)
        .build();
    PointSensitivityBuilder expected = ZeroRateSensitivity.of(GBP, LocalDate.of(2014, 7, 30), -0.99d * relativeTime);
    assertEquals(test.discountFactorZeroRateSensitivity(GBP, LocalDate.of(2014, 7, 30)), expected);
  }

  //-------------------------------------------------------------------------
  public void test_fxRate_separate() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxRate(USD, GBP), 1 / 1.6d, 0d);
    assertEquals(test.fxRate(USD, USD), 1d, 0d);
  }

  public void test_fxRate_pair() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxRate(CurrencyPair.of(USD, GBP)), 1 / 1.6d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_fxConvert() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .dayCount(ACT_ACT_ISDA)
        .build();
    MultiCurrencyAmount mca = MultiCurrencyAmount.of(
        CurrencyAmount.of(GBP, 100),
        CurrencyAmount.of(USD, 200));
    assertEquals(test.fxConvert(mca, GBP), CurrencyAmount.of(GBP, 100 + (1 / 1.6d) * 200));
  }

  //-------------------------------------------------------------------------
  public void test_fxIndexRate_beforeToday_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, GBP, PREV_DATE), 0.62d, 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, USD, PREV_DATE), 1d / 0.62d, 0d);
  }

  public void test_fxIndexRate_beforeToday_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrows(
        () -> test.fxIndexRate(WM_GBP_USD, USD, PREV_DATE),
        PricingException.class);
  }

  public void test_fxIndexRate_today_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, GBP, VAL_DATE), 0.62d, 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, USD, VAL_DATE), 1d / 0.62d, 0d);
  }

  public void test_fxIndexRate_today_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, GBP, VAL_DATE), 1.6d * (0.99d / 0.95d), 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, USD, VAL_DATE), (1d / 1.6d) * (0.95d / 0.99d), 0d);
  }

  public void test_fxIndexRate_afterToday() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, GBP, VAL_DATE), 1.6d * (0.99d / 0.95d), 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, USD, VAL_DATE), (1d / 1.6d) * (0.95d / 0.99d), 0d);
  }

  public void test_fxIndexRate_badCurrency() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrowsIllegalArg(() -> test.fxIndexRate(WM_GBP_USD, EUR, VAL_DATE));
  }

  //-------------------------------------------------------------------------
  public void test_iborIndexRate_beforeToday_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.0123d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, PREV_DATE), 0.0123d, 0d);
    assertEquals(test.iborIndexRateSensitivity(USD_LIBOR_3M, PREV_DATE), PointSensitivityBuilder.none());
  }

  public void test_iborIndexRate_beforeToday_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrows(
        () -> test.iborIndexRate(USD_LIBOR_3M, PREV_DATE),
        PricingException.class);
    // sensitivity succeeds, as result would be no sensitivity whether data is there or not
    assertEquals(test.iborIndexRateSensitivity(USD_LIBOR_3M, PREV_DATE), PointSensitivityBuilder.none());
  }

  public void test_iborIndexRate_today_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.0123d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, VAL_DATE), 0.0123d, 0d);
    assertEquals(test.iborIndexRateSensitivity(USD_LIBOR_3M, VAL_DATE), PointSensitivityBuilder.none());
  }

  public void test_iborIndexRate_today_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .indexCurves(ImmutableMap.of(USD_LIBOR_3M, USD_LIBOR_CURVE))
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, VAL_DATE), 0.0123d, 0d);

    PointSensitivityBuilder sens = IborRateSensitivity.of(USD_LIBOR_3M, VAL_DATE, 1.0);
    assertEquals(test.iborIndexRateSensitivity(USD_LIBOR_3M, VAL_DATE), sens);
  }

  public void test_iborIndexRate_afterToday() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .indexCurves(ImmutableMap.of(USD_LIBOR_3M, USD_LIBOR_CURVE))
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, NEXT_DATE), 0.0123d, 0d);

    PointSensitivityBuilder sens = IborRateSensitivity.of(USD_LIBOR_3M, LocalDate.of(2014, 7, 30), 1.0);
    assertEquals(test.iborIndexRateSensitivity(USD_LIBOR_3M, LocalDate.of(2014, 7, 30)), sens);
  }

  public void test_iborIndexRate_notKnown() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrowsIllegalArg(() -> test.iborIndexRate(USD_LIBOR_3M, NEXT_DATE));
  }

  //-------------------------------------------------------------------------
  public void test_overnightIndexRateFixing_beforePublication_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV2_DATE, 0.0123d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.overnightIndexRate(USD_FED_FUND, PREV2_DATE), 0.0123d, 0d);
    assertEquals(test.overnightIndexRateSensitivity(USD_FED_FUND, PREV2_DATE), PointSensitivityBuilder.none());
  }

  public void test_overnightIndexRateFixing_beforePublication_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrows(
        () -> test.overnightIndexRate(USD_FED_FUND, PREV2_DATE),
        PricingException.class);
    // sensitivity succeeds, as result would be no sensitivity whether data is there or not
    assertEquals(test.overnightIndexRateSensitivity(USD_FED_FUND, PREV2_DATE), PointSensitivityBuilder.none());
  }

  public void test_overnightIndexRateFixing_publication_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.0123d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.overnightIndexRate(USD_FED_FUND, PREV_DATE), 0.0123d, 0d);
    assertEquals(test.overnightIndexRateSensitivity(USD_FED_FUND, PREV_DATE), PointSensitivityBuilder.none());
  }

  public void test_overnightIndexRateFixing_publication_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .indexCurves(ImmutableMap.of(USD_FED_FUND, FED_FUND_CURVE))
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.overnightIndexRate(USD_FED_FUND, PREV_DATE), 0.0123d, 0d);
    PointSensitivityBuilder sens = OvernightRateSensitivity.of(USD_FED_FUND, PREV_DATE, 1d);
    assertEquals(test.overnightIndexRateSensitivity(USD_FED_FUND, PREV_DATE), sens);
  }

  public void test_overnightIndexRateFixing_afterPublication() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .indexCurves(ImmutableMap.of(USD_FED_FUND, FED_FUND_CURVE))
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.overnightIndexRate(USD_FED_FUND, NEXT_DATE), 0.0123d, 0d);
    PointSensitivityBuilder sens = OvernightRateSensitivity.of(USD_FED_FUND, NEXT_DATE, 1d);
    assertEquals(test.overnightIndexRateSensitivity(USD_FED_FUND, NEXT_DATE), sens);
  }

  //-------------------------------------------------------------------------
  public void test_overnightIndexRatePeriod_badDatesNotSorted() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrowsIllegalArg(() -> test.overnightIndexRatePeriod(USD_FED_FUND, NEXT_DATE, VAL_DATE));
    assertThrowsIllegalArg(() -> test.overnightIndexRatePeriodSensitivity(USD_FED_FUND, NEXT_DATE, VAL_DATE));
  }

  public void test_overnightIndexRatePeriod_BadDateInPast() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrowsIllegalArg(() -> test.overnightIndexRatePeriod(USD_FED_FUND, PREV2_DATE, PREV_DATE));
    assertThrowsIllegalArg(() -> test.overnightIndexRatePeriodSensitivity(USD_FED_FUND, PREV2_DATE, PREV_DATE));
  }

  public void test_overnightIndexRatePeriod_forward() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    LocalDate startDate = NEXT_DATE;
    LocalDate endDate = NEXT_DATE.plus(Period.ofMonths(3));
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .indexCurves(ImmutableMap.of(USD_FED_FUND, FED_FUND_CURVE))
        .timeSeries(ImmutableMap.of(USD_FED_FUND, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.overnightIndexRatePeriod(USD_FED_FUND, startDate, endDate), 0.0123d, 0d);
    PointSensitivityBuilder sens = OvernightRateSensitivity.of(USD_FED_FUND, USD, startDate, endDate, 1d);
    assertEquals(test.overnightIndexRatePeriodSensitivity(USD_FED_FUND, startDate, endDate), sens);
  }

  //-------------------------------------------------------------------------
  public void test_relativeTime() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.relativeTime(LocalDate.of(2014, 7, 30)),
        ACT_ACT_ISDA.yearFraction(VAL_DATE, LocalDate.of(2014, 7, 30)), 0d);
    assertEquals(test.relativeTime(LocalDate.of(2014, 5, 30)),
        -ACT_ACT_ISDA.yearFraction(LocalDate.of(2014, 5, 30), VAL_DATE), 0d);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .dayCount(ACT_ACT_ISDA)
        .build();
    coverImmutableBean(test);
    ImmutableRatesProvider test2 = ImmutableRatesProvider.builder()
        .valuationDate(LocalDate.of(2014, 6, 27))
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP))
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, LocalDateDoubleTimeSeries.empty()))
        .dayCount(ACT_365F)
        .build();
    coverBeanEquals(test, test2);
  }

}
