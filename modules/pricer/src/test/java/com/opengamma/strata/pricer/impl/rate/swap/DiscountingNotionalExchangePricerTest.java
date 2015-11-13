/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.rate.swap.NotionalExchange;

/**
 * Test.
 */
@Test
public class DiscountingNotionalExchangePricerTest {

  private static final LocalDate VAL_DATE = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate().minusDays(90);
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double TOLERANCE = 1.0e-10;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    double calculated = test.presentValue(NOTIONAL_EXCHANGE_REC_GBP, prov);
    assertEquals(calculated, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * DISCOUNT_FACTOR, 0d);
  }

  public void test_forecastValue() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    double calculated = test.forecastValue(NOTIONAL_EXCHANGE_REC_GBP, prov);
    assertEquals(calculated, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount(), 0d);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = test.presentValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, prov).build();

    double eps = 1.0e-7;
    PointSensitivities senseExpected = PointSensitivities.of(dscSensitivityFD(prov,
        NOTIONAL_EXCHANGE_REC_GBP, eps));
    assertTrue(senseComputed.equalWithTolerance(
        senseExpected, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * eps));
  }

  public void test_forecastValueSensitivity() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = test.forecastValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, prov).build();

    double eps = 1.0e-12;
    PointSensitivities senseExpected = PointSensitivities.empty();
    assertTrue(senseComputed.equalWithTolerance(
        senseExpected, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * eps));
  }

  private List<ZeroRateSensitivity> dscSensitivityFD(RatesProvider provider, NotionalExchange event, double eps) {
    Currency currency = event.getCurrency();
    LocalDate paymentDate = event.getPaymentDate();
    double discountFactor = provider.discountFactor(currency, paymentDate);
    double paymentTime = DAY_COUNT.relativeYearFraction(VAL_DATE, paymentDate);
    RatesProvider provUp = mock(RatesProvider.class);
    RatesProvider provDw = mock(RatesProvider.class);
    when(provUp.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    when(provDw.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(eps * paymentTime));
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    double pvUp = pricer.presentValue(event, provUp);
    double pvDw = pricer.presentValue(event, provDw);
    double res = 0.5 * (pvUp - pvDw) / eps;
    List<ZeroRateSensitivity> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(ZeroRateSensitivity.of(currency, paymentDate, res));
    return zeroRateSensi;
  }

  //-------------------------------------------------------------------------
  public void test_explainPresentValue() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);

    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(NOTIONAL_EXCHANGE_REC_GBP, prov, builder);
    ExplainMap explain = builder.build();

    Currency currency = NOTIONAL_EXCHANGE_REC_GBP.getCurrency();
    CurrencyAmount notional = NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount();
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "NotionalExchange");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), currency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount(), notional.getAmount(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.DISCOUNT_FACTOR).get(), DISCOUNT_FACTOR, TOLERANCE);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(), notional.getAmount(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(), notional.getAmount() * DISCOUNT_FACTOR, TOLERANCE);
  }

  public void test_explainPresentValue_paymentDateInPast() {
    SimpleRatesProvider prov = createProvider(NOTIONAL_EXCHANGE_REC_GBP);
    prov.setValuationDate(VAL_DATE.plusYears(1));

    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(NOTIONAL_EXCHANGE_REC_GBP, prov, builder);
    ExplainMap explain = builder.build();

    Currency currency = NOTIONAL_EXCHANGE_REC_GBP.getCurrency();
    CurrencyAmount notional = NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount();
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "NotionalExchange");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), currency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount(), notional.getAmount(), TOLERANCE);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(), 0d, TOLERANCE);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency(), currency);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(), 0d * DISCOUNT_FACTOR, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(NotionalExchange ne) {
    LocalDate paymentDate = ne.getPaymentDate();
    double paymentTime = DAY_COUNT.relativeYearFraction(VAL_DATE, ne.getPaymentDate());
    Currency currency = ne.getCurrency();

    DiscountFactors mockDf = mock(DiscountFactors.class);
    when(mockDf.discountFactor(paymentDate)).thenReturn(DISCOUNT_FACTOR);
    ZeroRateSensitivity sens = ZeroRateSensitivity.of(currency, paymentDate, -DISCOUNT_FACTOR * paymentTime);
    when(mockDf.zeroRatePointSensitivity(paymentDate)).thenReturn(sens);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE, mockDf);
    prov.setDayCount(DAY_COUNT);
    return prov;
  }

}
