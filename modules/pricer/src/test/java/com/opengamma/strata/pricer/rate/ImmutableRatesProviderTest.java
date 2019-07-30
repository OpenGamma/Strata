/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;

import java.time.LocalDate;

import org.joda.beans.Bean;
import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.ImmutablePriceIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.location.Country;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.discountFactors(GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.discountFactor(GBP, LocalDate.of(2014, 7, 30)));
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
    assertEquals(test.getTimeSeriesIndices(), ImmutableSet.of(GBP_USD_WM));
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
    assertEquals(test.getIborIndices(), ImmutableSet.of(USD_LIBOR_3M));
    assertEquals(test.getTimeSeriesIndices(), ImmutableSet.of(USD_LIBOR_3M));
  }

  public void test_iborIndexRates_activeNotFound() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrows(IllegalArgumentException.class, () -> test.iborIndexRates(USD_LIBOR_3M));
  }

  public void test_iborIndexRates_inactive() {
    IborIndex inactiveIndex = IborIndex.of("USD-LIBOR-10M");
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .timeSeries(inactiveIndex, ts)
        .build();
    assertEquals(test.iborIndexRates(inactiveIndex).getIndex(), inactiveIndex);
    assertEquals(test.iborIndexRates(inactiveIndex).getFixings(), ts);
    assertEquals(test.getIborIndices(), ImmutableSet.of());
    assertEquals(test.getTimeSeriesIndices(), ImmutableSet.of(inactiveIndex));
    assertEquals(test.iborIndexRates(inactiveIndex).getClass(), HistoricIborIndexRates.class);
  }

  public void test_iborIndexRates_inactiveNoTimeSeriesNotFound() {
    IborIndex inactiveIndex = IborIndex.of("USD-LIBOR-10M");
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrows(IllegalArgumentException.class, () -> test.iborIndexRates(inactiveIndex));
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
    assertEquals(test.getOvernightIndices(), ImmutableSet.of(USD_FED_FUND));
    assertEquals(test.getTimeSeriesIndices(), ImmutableSet.of(USD_FED_FUND));
  }

  public void test_overnightIndexRates_activeNotFound() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrows(IllegalArgumentException.class, () -> test.overnightIndexRates(USD_FED_FUND));
  }

  public void test_overnightIndexRates_inactive() {
    OvernightIndex inactiveIndex = OvernightIndex.of("CHF-TOIS");
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .timeSeries(inactiveIndex, ts)
        .build();
    assertEquals(test.overnightIndexRates(inactiveIndex).getIndex(), inactiveIndex);
    assertEquals(test.overnightIndexRates(inactiveIndex).getFixings(), ts);
    assertEquals(test.getIborIndices(), ImmutableSet.of());
    assertEquals(test.getTimeSeriesIndices(), ImmutableSet.of(inactiveIndex));
    assertEquals(test.overnightIndexRates(inactiveIndex).getClass(), HistoricOvernightIndexRates.class);
  }

  public void test_overnightIndexRates_inactiveNoTimeSeriesNotFound() {
    OvernightIndex inactiveIndex = OvernightIndex.of("CHF-TOIS");
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrows(IllegalArgumentException.class, () -> test.overnightIndexRates(inactiveIndex));
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
    assertEquals(test.getPriceIndices(), ImmutableSet.of(GB_RPI));
    assertEquals(test.getTimeSeriesIndices(), ImmutableSet.of(GB_RPI));
  }

  public void test_priceIndexValues_activeNotFound() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrows(IllegalArgumentException.class, () -> test.priceIndexValues(GB_RPI));
  }

  public void test_priceIndexValues_inactive() {
    PriceIndex inactiveIndex = ImmutablePriceIndex.builder()
        .name("GBP-XXX")
        .active(false)
        .publicationFrequency(Frequency.P1M)
        .currency(GBP)
        .region(Country.GB)
        .build();
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(VAL_DATE, 0.62d);
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .timeSeries(inactiveIndex, ts)
        .build();
    assertEquals(test.priceIndexValues(inactiveIndex).getIndex(), inactiveIndex);
    assertEquals(test.priceIndexValues(inactiveIndex).getFixings(), ts);
    assertEquals(test.getIborIndices(), ImmutableSet.of());
    assertEquals(test.getTimeSeriesIndices(), ImmutableSet.of(inactiveIndex));
    assertEquals(test.priceIndexValues(inactiveIndex).getClass(), HistoricPriceIndexValues.class);
  }

  public void test_priceIndexValues_inactiveNoTimeSeriesNotFound() {
    PriceIndex inactiveIndex = ImmutablePriceIndex.builder()
        .name("GBP-XXX")
        .active(false)
        .publicationFrequency(Frequency.P1M)
        .currency(GBP)
        .region(Country.GB)
        .build();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .build();
    assertThrows(IllegalArgumentException.class, () -> test.priceIndexValues(inactiveIndex));
  }

  //-------------------------------------------------------------------------
  public void test_getCurves() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    assertEquals(test.getCurves().size(), 2);
    assertEquals(test.getCurves().get(DISCOUNT_CURVE_GBP.getName()), DISCOUNT_CURVE_GBP);
    assertEquals(test.getCurves().get(DISCOUNT_CURVE_USD.getName()), DISCOUNT_CURVE_USD);
  }

  public void test_getCurves_withGroup() {
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    CurveGroupName group = CurveGroupName.of("GRP");
    assertEquals(test.getCurves(group).size(), 2);
    assertEquals(test.getCurves(group).get(CurveId.of(group, DISCOUNT_CURVE_GBP.getName())), DISCOUNT_CURVE_GBP);
    assertEquals(test.getCurves(group).get(CurveId.of(group, DISCOUNT_CURVE_USD.getName())), DISCOUNT_CURVE_USD);
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
