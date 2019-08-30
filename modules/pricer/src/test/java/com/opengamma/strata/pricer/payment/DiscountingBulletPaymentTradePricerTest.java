/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.payment;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.BaseProvider;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.payment.ResolvedBulletPayment;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Test {@link DiscountingBulletPaymentTradePricer}.
 */
public class DiscountingBulletPaymentTradePricerTest {

  private static final DiscountingBulletPaymentTradePricer PRICER = DiscountingBulletPaymentTradePricer.DEFAULT;
  private static final double DF = 0.96d;
  private static final LocalDate VAL_DATE_2014_01_22 = RatesProviderFxDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate PAYMENT_DATE = VAL_DATE_2014_01_22.plusWeeks(8);
  private static final LocalDate PAYMENT_DATE_PAST = VAL_DATE_2014_01_22.minusDays(1);
  private static final double NOTIONAL_USD = 100_000_000;
  private static final CurrencyAmount AMOUNT = CurrencyAmount.of(USD, NOTIONAL_USD);
  private static final ResolvedBulletPaymentTrade TRADE = ResolvedBulletPaymentTrade.of(
      TradeInfo.empty(), ResolvedBulletPayment.of(Payment.of(AMOUNT, PAYMENT_DATE)));
  private static final ResolvedBulletPaymentTrade TRADE_PAST = ResolvedBulletPaymentTrade.of(
      TradeInfo.empty(), ResolvedBulletPayment.of(Payment.of(AMOUNT, PAYMENT_DATE_PAST)));

  private static final ConstantCurve CURVE = ConstantCurve.of(Curves.discountFactors("Test", ACT_365F), DF);
  private static final SimpleDiscountFactors DISCOUNT_FACTORS = SimpleDiscountFactors.of(USD, VAL_DATE_2014_01_22, CURVE);
  private static final BaseProvider PROVIDER = new SimpleRatesProvider(VAL_DATE_2014_01_22, DISCOUNT_FACTORS);
  private static final double TOL = 1.0e-12;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue_provider() {
    CurrencyAmount computed = PRICER.presentValue(TRADE, PROVIDER);
    double expected = NOTIONAL_USD * DF;
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValue_provider_ended() {
    CurrencyAmount computed = PRICER.presentValue(TRADE_PAST, PROVIDER);
    assertThat(computed).isEqualTo(CurrencyAmount.zero(USD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explainPresentValue_provider() {
    CurrencyAmount fvExpected = AMOUNT;
    CurrencyAmount pvExpected = PRICER.presentValue(TRADE, PROVIDER);

    ExplainMap explain = PRICER.explainPresentValue(TRADE, PROVIDER);
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("Payment");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_DATE);
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(USD);
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DF, offset(TOL));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(USD);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(fvExpected.getAmount(), offset(TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(USD);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(pvExpected.getAmount(), offset(TOL));
  }

  @Test
  public void test_explainPresentValue_provider_ended() {
    ExplainMap explain = PRICER.explainPresentValue(TRADE_PAST, PROVIDER);
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("Payment");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_DATE_PAST);
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(USD);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(USD);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0, offset(TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(USD);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_provider() {
    PointSensitivities point = PRICER.presentValueSensitivity(TRADE, PROVIDER);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = -DF * relativeYearFraction * NOTIONAL_USD;
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertThat(actual.getCurrency()).isEqualTo(USD);
    assertThat(actual.getCurveCurrency()).isEqualTo(USD);
    assertThat(actual.getYearFraction()).isEqualTo(relativeYearFraction);
    assertThat(actual.getSensitivity()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValueSensitivity_provider_ended() {
    PointSensitivities computed = PRICER.presentValueSensitivity(TRADE_PAST, PROVIDER);
    assertThat(computed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cashFlow_provider() {
    CashFlow expected = CashFlow.ofForecastValue(PAYMENT_DATE, USD, NOTIONAL_USD, DF);
    assertThat(PRICER.cashFlows(TRADE, PROVIDER)).isEqualTo(CashFlows.of(expected));
  }

  @Test
  public void test_cashFlow_provider_ended() {
    assertThat(PRICER.cashFlows(TRADE_PAST, PROVIDER)).isEqualTo(CashFlows.NONE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    assertThat(PRICER.currencyExposure(TRADE, PROVIDER)).isEqualTo(PRICER.presentValue(TRADE, PROVIDER));
  }

  @Test
  public void test_currentCash_onDate() {
    SimpleRatesProvider prov = new SimpleRatesProvider(PAYMENT_DATE, DISCOUNT_FACTORS);
    assertThat(PRICER.currentCash(TRADE, prov)).isEqualTo(AMOUNT);
  }

  @Test
  public void test_currentCash_past() {
    assertThat(PRICER.currentCash(TRADE_PAST, PROVIDER)).isEqualTo(CurrencyAmount.zero(USD));
  }

}
