/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.pricer.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.swap.NotionalExchange;

/**
 * Test.
 */
public class DiscountingNotionalExchangePricerTest {

  private static final LocalDate VAL_DATE = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate().minusDays(90);
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double TOLERANCE = 1.0e-10;

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.DOUBLE_QUADRATIC;
  private static final Curve DISCOUNT_CURVE_GBP;
  static {
    DoubleArray timeGbp = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
    DoubleArray rateGbp = DoubleArray.of(0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210);
    DISCOUNT_CURVE_GBP = InterpolatedNodalCurve.of(
        Curves.zeroRates("GBP-Discount", ACT_ACT_ISDA), timeGbp, rateGbp, INTERPOLATOR);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    double calculated = test.presentValue(NOTIONAL_EXCHANGE_REC_GBP, prov);
    assertThat(calculated).isCloseTo(NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * DISCOUNT_FACTOR, offset(0d));
  }

  @Test
  public void test_forecastValue() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    double calculated = test.forecastValue(NOTIONAL_EXCHANGE_REC_GBP, prov);
    assertThat(calculated).isCloseTo(NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount(), offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = test.presentValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, prov).build();

    double eps = 1.0e-7;
    PointSensitivities senseExpected = PointSensitivities.of(dscSensitivityFD(prov, NOTIONAL_EXCHANGE_REC_GBP, eps));
    assertThat(senseComputed.equalWithTolerance(
        senseExpected, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * eps)).isTrue();
  }

  @Test
  public void test_forecastValueSensitivity() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = test.forecastValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, prov).build();

    double eps = 1.0e-12;
    PointSensitivities senseExpected = PointSensitivities.empty();
    assertThat(senseComputed.equalWithTolerance(
        senseExpected, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * eps)).isTrue();
  }

  private List<ZeroRateSensitivity> dscSensitivityFD(RatesProvider provider, NotionalExchange event, double eps) {
    Currency currency = event.getCurrency();
    LocalDate paymentDate = event.getPaymentDate();
    double discountFactor = provider.discountFactor(currency, paymentDate);
    double paymentTime = DAY_COUNT.relativeYearFraction(VAL_DATE, paymentDate);
    RatesProvider provUp = mock(RatesProvider.class);
    RatesProvider provDw = mock(RatesProvider.class);
    when(provUp.getValuationDate()).thenReturn(VAL_DATE);
    when(provUp.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    when(provDw.getValuationDate()).thenReturn(VAL_DATE);
    when(provDw.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(eps * paymentTime));
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    double pvUp = pricer.presentValue(event, provUp);
    double pvDw = pricer.presentValue(event, provDw);
    double res = 0.5 * (pvUp - pvDw) / eps;
    List<ZeroRateSensitivity> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(ZeroRateSensitivity.of(currency, paymentTime, res));
    return zeroRateSensi;
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explainPresentValue() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(NOTIONAL_EXCHANGE_REC_GBP, prov, builder);
    ExplainMap explain = builder.build();

    Currency currency = NOTIONAL_EXCHANGE_REC_GBP.getCurrency();
    CurrencyAmount notional = NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("NotionalExchange");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(notional.getAmount(), offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DISCOUNT_FACTOR, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(notional.getAmount(), offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(notional.getAmount() * DISCOUNT_FACTOR, offset(TOLERANCE));
  }

  @Test
  public void test_explainPresentValue_paymentDateInPast() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);
    prov.setValuationDate(VAL_DATE.plusYears(1));

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(NOTIONAL_EXCHANGE_REC_GBP, prov, builder);
    ExplainMap explain = builder.build();

    Currency currency = NOTIONAL_EXCHANGE_REC_GBP.getCurrency();
    CurrencyAmount notional = NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("NotionalExchange");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(notional.getAmount(), offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0d, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0d * DISCOUNT_FACTOR, offset(TOLERANCE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    MultiCurrencyAmount computed = test.currencyExposure(NOTIONAL_EXCHANGE_REC_GBP, prov);
    PointSensitivities point = test.presentValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, prov).build();
    MultiCurrencyAmount expected = prov.currencyExposure(point).plus(
        CurrencyAmount.of(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(), test.presentValue(NOTIONAL_EXCHANGE_REC_GBP, prov)));
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_currentCash_zero() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    double computed = test.currentCash(NOTIONAL_EXCHANGE_REC_GBP, prov);
    assertThat(computed).isEqualTo(0d);
  }

  @Test
  public void test_currentCash_onPayment() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate())
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    double notional = NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount();
    double computed = test.currentCash(NOTIONAL_EXCHANGE_REC_GBP, prov);
    assertThat(computed).isEqualTo(notional);
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(NotionalExchange ne) {
    LocalDate paymentDate = ne.getPaymentDate();
    double paymentTime = DAY_COUNT.relativeYearFraction(VAL_DATE, paymentDate);
    Currency currency = ne.getCurrency();

    DiscountFactors mockDf = mock(DiscountFactors.class);
    when(mockDf.discountFactor(paymentDate)).thenReturn(DISCOUNT_FACTOR);
    ZeroRateSensitivity sens = ZeroRateSensitivity.of(currency, paymentTime, -DISCOUNT_FACTOR * paymentTime);
    when(mockDf.zeroRatePointSensitivity(paymentDate)).thenReturn(sens);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE, mockDf);
    prov.setDayCount(DAY_COUNT);
    return prov;
  }

}
