/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.joda.beans.Bean;
import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.fx.DiscountFxForwardRates;

/**
 * Test {@link ImmutableRatesProvider}.
 */
@Test
public class ImmutableRatesProviderTest {

  private static final LocalDate PREV_DATE = LocalDate.of(2014, 6, 27);
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final double FX_GBP_USD = 1.6d;
  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, FX_GBP_USD);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;

  private static final double GBP_DSC = 0.99d;
  private static final double USD_DSC = 0.95d;
  private static final Curve DISCOUNT_CURVE_GBP = ConstantCurve.of(
      Curves.zeroRates("GBP-Discount", ACT_ACT_ISDA), GBP_DSC);
  private static final Curve DISCOUNT_CURVE_USD = ConstantCurve.of(
      Curves.zeroRates("USD-Discount", ACT_ACT_ISDA), USD_DSC);
  private static final Curve USD_LIBOR_CURVE = ConstantCurve.of(
      Curves.zeroRates("USD-Discount", ACT_ACT_ISDA), 0.96d);
  private static final Curve FED_FUND_CURVE = ConstantCurve.of(
      Curves.zeroRates("USD-Discount", ACT_ACT_ISDA), 0.97d);
  private static final Curve GBPRI_CURVE = InterpolatedNodalCurve.of(
      Curves.prices("GB-RPI"), DoubleArray.of(1d, 10d), DoubleArray.of(252d, 252d), INTERPOLATOR);

  //-------------------------------------------------------------------------
  public void test_builder() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .timeSeries(GBP_USD_WM, ts)
        .build();
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(ImmutableRatesProvider.meta().timeSeries().get(test), ImmutableMap.of(GBP_USD_WM, ts));
    assertSame(test.toImmutableRatesProvider(), test);
  }

  //-------------------------------------------------------------------------
  public void test_discountFactors() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    assertEquals(test.discountFactors(GBP).getCurrency(), GBP);
  }

  public void test_discountFactors_notKnown() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrowsIllegalArg(() -> test.discountFactors(GBP));
    assertThrowsIllegalArg(() -> test.discountFactor(GBP, LocalDate.of(2014, 7, 30)));
  }

  //-------------------------------------------------------------------------
  public void test_fxRate_separate() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .build();
    assertEquals(test.fxRate(USD, GBP), 1 / FX_GBP_USD, 0d);
    assertEquals(test.fxRate(USD, USD), 1d, 0d);
  }

  public void test_fxRate_pair() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .build();
    assertEquals(test.fxRate(CurrencyPair.of(USD, GBP)), 1 / FX_GBP_USD, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_fxIndexRates() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .timeSeries(GBP_USD_WM, ts)
        .build();
    assertEquals(test.fxIndexRates(GBP_USD_WM).getIndex(), GBP_USD_WM);
    assertEquals(test.fxIndexRates(GBP_USD_WM).getFixings(), ts);
  }

  //-------------------------------------------------------------------------
  public void test_fxForwardRates() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    DiscountFxForwardRates res = (DiscountFxForwardRates) test.fxForwardRates(CurrencyPair.of(GBP, USD));
    assertEquals(res.getBaseCurrencyDiscountFactors(), ZeroRateDiscountFactors.of(GBP, VAL_DATE, DISCOUNT_CURVE_GBP));
    assertEquals(res.getCounterCurrencyDiscountFactors(), ZeroRateDiscountFactors.of(USD, VAL_DATE, DISCOUNT_CURVE_USD));
    assertEquals(res.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(res.getFxRateProvider(), FX_MATRIX);
    assertEquals(res.getValuationDate(), VAL_DATE);
  }

  //-------------------------------------------------------------------------
  public void test_iborIndexRates() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .iborIndexCurve(USD_LIBOR_3M, USD_LIBOR_CURVE)
        .timeSeries(USD_LIBOR_3M, ts)
        .build();
    assertEquals(test.iborIndexRates(USD_LIBOR_3M).getIndex(), USD_LIBOR_3M);
    assertEquals(test.iborIndexRates(USD_LIBOR_3M).getFixings(), ts);
  }

  //-------------------------------------------------------------------------
  public void test_overnightIndexRates() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .overnightIndexCurve(USD_FED_FUND, FED_FUND_CURVE)
        .timeSeries(USD_FED_FUND, ts)
        .build();
    assertEquals(test.overnightIndexRates(USD_FED_FUND).getIndex(), USD_FED_FUND);
    assertEquals(test.overnightIndexRates(USD_FED_FUND).getFixings(), ts);
  }

  //-------------------------------------------------------------------------
  public void test_priceIndexValues() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .priceIndexCurve(GB_RPI, GBPRI_CURVE)
        .timeSeries(GB_RPI, ts)
        .build();
    assertEquals(test.priceIndexValues(GB_RPI).getIndex(), GB_RPI);
    assertEquals(test.priceIndexValues(GB_RPI).getFixings(), ts);
  }

  public void test_priceIndexValues_notKnown() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrowsIllegalArg(() -> test.priceIndexValues(GB_RPI));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    coverImmutableBean(test);
    ImmutableRatesProvider test2 = ImmutableRatesProvider.builder(LocalDate.of(2014, 6, 27))
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    coverBeanEquals(test, test2);
  }
  
  public void testSerializeDeserialize() {
    cycleBean(ImmutableRatesProvider.builder(VAL_DATE).build());
  }
  
  private void cycleBean(Bean bean) {
    JodaBeanSer ser = JodaBeanSer.COMPACT;
    String result = ser.xmlWriter().write(bean);
    Bean cycled = ser.xmlReader().read(result);
    assertThat(cycled).isEqualTo(bean);
  }


}
