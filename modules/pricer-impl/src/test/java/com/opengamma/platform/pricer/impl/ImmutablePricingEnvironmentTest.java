/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import static com.opengamma.basics.currency.Currency.EUR;
import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.DayCounts.ACT_365F;
import static com.opengamma.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.basics.index.IborIndices;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test.
 */
@Test
public class ImmutablePricingEnvironmentTest {

  private static final com.opengamma.util.money.Currency OLD_GBP = com.opengamma.util.money.Currency.GBP;
  private static final com.opengamma.util.money.Currency OLD_USD = com.opengamma.util.money.Currency.USD;
  private static final LocalDate PREV_DATE = LocalDate.of(2014, 6, 27);
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final LocalDate NEXT_DATE = LocalDate.of(2014, 7, 1);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(ImmutablePricingEnvironment.meta().multicurve().get(test), SwapMockData.MULTICURVE_OIS);
    assertEquals(ImmutablePricingEnvironment.meta().timeSeries().get(test), SwapMockData.TIME_SERIES);
    assertEquals(ImmutablePricingEnvironment.meta().dayCount().get(test), ACT_ACT_ISDA);
  }

  //-------------------------------------------------------------------------
  public void test_rawData() {
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.rawData(MulticurveProviderInterface.class), SwapMockData.MULTICURVE_OIS);
    assertThrowsIllegalArg(() -> test.rawData(Object.class));
  }

  //-------------------------------------------------------------------------
  public void test_timeSeries() {
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.timeSeries(IborIndices.USD_LIBOR_3M), SwapMockData.TS_USDLIBOR3M);
    assertThrowsIllegalArg(() -> test.timeSeries(IborIndices.CHF_LIBOR_1W));
  }

  //-------------------------------------------------------------------------
  public void test_discountFactor() {
    double dayCount = ACT_ACT_ISDA.yearFraction(VAL_DATE, LocalDate.of(2014, 7, 30));
    MulticurveProviderInterface mock = Mockito.mock(MulticurveProviderInterface.class);
    Mockito.when(mock.getDiscountFactor(OLD_GBP, dayCount)).thenReturn(0.99d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(mock)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.discountFactor(GBP, LocalDate.of(2014, 7, 30)), 0.99d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_fxRate() {
    MulticurveProviderInterface mock = Mockito.mock(MulticurveProviderInterface.class);
    Mockito.when(mock.getFxRate(OLD_USD, OLD_GBP)).thenReturn(0.62d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(mock)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxRate(CurrencyPair.of(USD, GBP)), 0.62d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_fxConvert() {
    MulticurveProviderInterface mock = Mockito.mock(MulticurveProviderInterface.class);
    Mockito.when(mock.getFxRate(OLD_USD, OLD_GBP)).thenReturn(0.62d);
    Mockito.when(mock.getFxRate(OLD_GBP, OLD_GBP)).thenReturn(1d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(mock)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    MultiCurrencyAmount mca = MultiCurrencyAmount.of(
        CurrencyAmount.of(GBP, 100),
        CurrencyAmount.of(USD, 200));
    assertEquals(test.fxConvert(mca, GBP), CurrencyAmount.of(GBP, 100 + 62 * 2));
  }

  //-------------------------------------------------------------------------
  public void test_fxIndexRate_beforeToday_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.62d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(GBP, USD), PREV_DATE), 0.62d, 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(USD, GBP), PREV_DATE), 1d / 0.62d, 0d);
  }

  public void test_fxIndexRate_beforeToday_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.EMPTY_SERIES;
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrows(
        () -> test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(USD, GBP), PREV_DATE),
        PricingException.class);
  }

  public void test_fxIndexRate_today_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(GBP, USD), VAL_DATE), 0.62d, 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(USD, GBP), VAL_DATE), 1d / 0.62d, 0d);
  }

  public void test_fxIndexRate_today_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.EMPTY_SERIES;
    MulticurveProviderInterface mock = Mockito.mock(MulticurveProviderInterface.class);
    double future = ACT_ACT_ISDA.yearFraction(VAL_DATE, WM_GBP_USD.calculateMaturityFromFixing(VAL_DATE));
    Mockito.when(mock.getDiscountFactor(OLD_GBP, future)).thenReturn(0.95d);
    Mockito.when(mock.getDiscountFactor(OLD_USD, future)).thenReturn(0.99d);
    Mockito.when(mock.getFxRate(OLD_GBP, OLD_USD)).thenReturn(1.6d);
    Mockito.when(mock.getFxRate(OLD_USD, OLD_GBP)).thenReturn(1 / 1.6d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(mock)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(GBP, USD), VAL_DATE), 1.6d * (0.95d / 0.99d), 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(USD, GBP), VAL_DATE), (1d / 1.6d) * (0.99d / 0.95d), 0d);
  }

  public void test_fxIndexRate_afterToday() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.EMPTY_SERIES;
    MulticurveProviderInterface mock = Mockito.mock(MulticurveProviderInterface.class);
    double future = ACT_ACT_ISDA.yearFraction(VAL_DATE, WM_GBP_USD.calculateMaturityFromFixing(NEXT_DATE));
    Mockito.when(mock.getDiscountFactor(OLD_GBP, future)).thenReturn(0.95d);
    Mockito.when(mock.getDiscountFactor(OLD_USD, future)).thenReturn(0.99d);
    Mockito.when(mock.getFxRate(OLD_GBP, OLD_USD)).thenReturn(1.6d);
    Mockito.when(mock.getFxRate(OLD_USD, OLD_GBP)).thenReturn(1 / 1.6d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(mock)
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(GBP, USD), NEXT_DATE), 1.6d * (0.95d / 0.99d), 0d);
    assertEquals(test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(USD, GBP), NEXT_DATE), (1d / 1.6d) * (0.99d / 0.95d), 0d);
  }

  public void test_fxIndexRate_badCurrencyPair() {
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrowsIllegalArg(() -> test.fxIndexRate(WM_GBP_USD, CurrencyPair.of(USD, EUR), VAL_DATE));
  }

  //-------------------------------------------------------------------------
  public void test_iborIndexRate_beforeToday_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.0123d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, PREV_DATE), 0.0123d, 0d);
  }

  public void test_iborIndexRate_beforeToday_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.EMPTY_SERIES;
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertThrows(
        () -> test.iborIndexRate(USD_LIBOR_3M, PREV_DATE),
        PricingException.class);
  }

  public void test_iborIndexRate_today_inTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.0123d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, VAL_DATE), 0.0123d, 0d);
  }

  public void test_iborIndexRate_today_notInTimeSeries() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.EMPTY_SERIES;
    MulticurveProviderInterface mock = Mockito.mock(MulticurveProviderInterface.class);
    LocalDate effectiveDate = USD_LIBOR_3M.calculateEffectiveFromFixing(VAL_DATE);
    LocalDate maturityDate = USD_LIBOR_3M.calculateMaturityFromEffective(effectiveDate);
    double effective = ACT_ACT_ISDA.yearFraction(VAL_DATE, effectiveDate);
    double maturity = ACT_ACT_ISDA.yearFraction(VAL_DATE, maturityDate);
    double yearFraction = USD_LIBOR_3M.getDayCount().yearFraction(effectiveDate, maturityDate);
    Mockito.when(mock.getSimplyCompoundForwardRate(
        Legacy.iborIndex(USD_LIBOR_3M), effective, maturity, yearFraction)).thenReturn(0.0123d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(mock)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, VAL_DATE), 0.0123d, 0d);
  }

  public void test_iborIndexRate_afterToday() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.EMPTY_SERIES;
    MulticurveProviderInterface mock = Mockito.mock(MulticurveProviderInterface.class);
    LocalDate effectiveDate = USD_LIBOR_3M.calculateEffectiveFromFixing(NEXT_DATE);
    LocalDate maturityDate = USD_LIBOR_3M.calculateMaturityFromEffective(effectiveDate);
    double effective = ACT_ACT_ISDA.yearFraction(VAL_DATE, effectiveDate);
    double maturity = ACT_ACT_ISDA.yearFraction(VAL_DATE, maturityDate);
    double yearFraction = USD_LIBOR_3M.getDayCount().yearFraction(effectiveDate, maturityDate);
    Mockito.when(mock.getSimplyCompoundForwardRate(
        Legacy.iborIndex(USD_LIBOR_3M), effective, maturity, yearFraction)).thenReturn(0.0123d);
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(mock)
        .timeSeries(ImmutableMap.of(USD_LIBOR_3M, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.iborIndexRate(USD_LIBOR_3M, NEXT_DATE), 0.0123d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_relativeTime() {
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertEquals(test.relativeTime(LocalDate.of(2014, 7, 30)),
        ACT_ACT_ISDA.yearFraction(VAL_DATE, LocalDate.of(2014, 7, 30)), 0d);
    assertEquals(test.relativeTime(LocalDate.of(2014, 5, 30)),
        -ACT_ACT_ISDA.yearFraction(LocalDate.of(2014, 5, 30), VAL_DATE), 0d);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_OIS)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    coverImmutableBean(test);
    ImmutablePricingEnvironment test2 = ImmutablePricingEnvironment.builder()
        .valuationDate(LocalDate.of(2014, 6, 27))
        .multicurve(SwapMockData.MULTICURVE_OIS2)
        .timeSeries(SwapMockData.TIME_SERIES_ON)
        .dayCount(ACT_365F)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ImmutablePricingEnvironment test = ImmutablePricingEnvironment.builder()
        .valuationDate(VAL_DATE)
        .multicurve(SwapMockData.MULTICURVE_SERIALIZABLE)
        .timeSeries(SwapMockData.TIME_SERIES)
        .dayCount(ACT_ACT_ISDA)
        .build();
    assertSerialization(test);
  }

}
