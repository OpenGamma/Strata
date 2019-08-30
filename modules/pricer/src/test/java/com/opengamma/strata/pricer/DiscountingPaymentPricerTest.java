/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.pricer.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.pricer.CompoundedRateType.PERIODIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;

/**
 * Test {@link DiscountingPaymentPricer}.
 */
public class DiscountingPaymentPricerTest {

  private static final DiscountingPaymentPricer PRICER = DiscountingPaymentPricer.DEFAULT;
  private static final double DF = 0.96d;
  private static final Currency USD = Currency.USD;
  private static final LocalDate VAL_DATE_2014_01_22 = RatesProviderFxDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate PAYMENT_DATE = VAL_DATE_2014_01_22.plusWeeks(8);
  private static final LocalDate PAYMENT_DATE_PAST = VAL_DATE_2014_01_22.minusDays(1);
  private static final double NOTIONAL_USD = 100_000_000;
  private static final Payment PAYMENT = Payment.of(CurrencyAmount.of(USD, NOTIONAL_USD), PAYMENT_DATE);
  private static final Payment PAYMENT_PAST = Payment.of(CurrencyAmount.of(USD, NOTIONAL_USD), PAYMENT_DATE_PAST);

  private static final ConstantCurve CURVE = ConstantCurve.of(Curves.discountFactors("Test", ACT_365F), DF);
  private static final SimpleDiscountFactors DISCOUNT_FACTORS = SimpleDiscountFactors.of(USD, VAL_DATE_2014_01_22, CURVE);
  private static final BaseProvider PROVIDER = new SimpleRatesProvider(VAL_DATE_2014_01_22, DISCOUNT_FACTORS);
  private static final double Z_SPREAD = 0.02;
  private static final int PERIOD_PER_YEAR = 4;
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue_provider() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT, PROVIDER);
    double expected = NOTIONAL_USD * DF;
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValue_provider_ended() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT_PAST, PROVIDER);
    assertThat(computed).isEqualTo(CurrencyAmount.zero(USD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue_df() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT, DISCOUNT_FACTORS);
    double expected = NOTIONAL_USD * DF;
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValue_df_ended() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT_PAST, DISCOUNT_FACTORS);
    assertThat(computed).isEqualTo(CurrencyAmount.zero(USD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueAmount_provider() {
    double computed = PRICER.presentValueAmount(PAYMENT, PROVIDER);
    double expected = NOTIONAL_USD * DF;
    assertThat(computed).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValueAmount_provider_ended() {
    double computed = PRICER.presentValueAmount(PAYMENT_PAST, PROVIDER);
    assertThat(computed).isCloseTo(0d, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueWithSpread_df_spread_continuous() {
    CurrencyAmount computed = PRICER
        .presentValueWithSpread(PAYMENT, DISCOUNT_FACTORS, Z_SPREAD, CONTINUOUS, 0);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = NOTIONAL_USD * DF * Math.exp(-Z_SPREAD * relativeYearFraction);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValueWithSpread_df_spread_periodic() {
    CurrencyAmount computed = PRICER.presentValueWithSpread(
        PAYMENT, DISCOUNT_FACTORS, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double rate = (Math.pow(DF, -1d / PERIOD_PER_YEAR / relativeYearFraction) - 1d) * PERIOD_PER_YEAR;
    double expected = NOTIONAL_USD *
        discountFactorFromPeriodicallyCompoundedRate(rate + Z_SPREAD, PERIOD_PER_YEAR, relativeYearFraction);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValueWithSpread_df_ended_spread() {
    CurrencyAmount computed = PRICER.presentValueWithSpread(PAYMENT_PAST, DISCOUNT_FACTORS, Z_SPREAD, PERIODIC, 3);
    assertThat(computed).isEqualTo(CurrencyAmount.zero(USD));
  }

  private double discountFactorFromPeriodicallyCompoundedRate(double rate, double periodPerYear, double time) {
    return Math.pow(1d + rate / periodPerYear, -periodPerYear * time);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explainPresentValue_provider() {
    CurrencyAmount fvExpected = PRICER.forecastValue(PAYMENT, PROVIDER);
    CurrencyAmount pvExpected = PRICER.presentValue(PAYMENT, PROVIDER);

    ExplainMap explain = PRICER.explainPresentValue(PAYMENT, PROVIDER);
    Currency currency = PAYMENT.getCurrency();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("Payment");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT.getDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DF, offset(TOL));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(fvExpected.getAmount(), offset(TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(pvExpected.getAmount(), offset(TOL));
  }

  @Test
  public void test_explainPresentValue_provider_ended() {
    ExplainMap explain = PRICER.explainPresentValue(PAYMENT_PAST, PROVIDER);
    Currency currency = PAYMENT_PAST.getCurrency();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("Payment");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(PAYMENT_PAST.getDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0, offset(TOL));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(currency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_provider() {
    PointSensitivities point = PRICER.presentValueSensitivity(PAYMENT, PROVIDER).build();
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
    PointSensitivities computed = PRICER.presentValueSensitivity(PAYMENT_PAST, PROVIDER).build();
    assertThat(computed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_df() {
    PointSensitivities point = PRICER.presentValueSensitivity(PAYMENT, DISCOUNT_FACTORS).build();
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = -DF * relativeYearFraction * NOTIONAL_USD;
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertThat(actual.getCurrency()).isEqualTo(USD);
    assertThat(actual.getCurveCurrency()).isEqualTo(USD);
    assertThat(actual.getYearFraction()).isEqualTo(relativeYearFraction);
    assertThat(actual.getSensitivity()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValueSensitivity_df_ended() {
    PointSensitivities computed = PRICER.presentValueSensitivity(PAYMENT_PAST, DISCOUNT_FACTORS).build();
    assertThat(computed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivityWithSpread_df_spread_continuous() {
    PointSensitivities point = PRICER.presentValueSensitivityWithSpread(
        PAYMENT, DISCOUNT_FACTORS, Z_SPREAD, CONTINUOUS, 0).build();
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = -DF * relativeYearFraction * NOTIONAL_USD * Math.exp(-Z_SPREAD * relativeYearFraction);
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertThat(actual.getCurrency()).isEqualTo(USD);
    assertThat(actual.getCurveCurrency()).isEqualTo(USD);
    assertThat(actual.getYearFraction()).isEqualTo(relativeYearFraction);
    assertThat(actual.getSensitivity()).isCloseTo(expected, offset(NOTIONAL_USD * TOL));
  }

  @Test
  public void test_presentValueSensitivityWithSpread_df_spread_periodic() {
    PointSensitivities point = PRICER.presentValueSensitivityWithSpread(
        PAYMENT, DISCOUNT_FACTORS, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR).build();
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double discountFactorUp = DF * Math.exp(-EPS * relativeYearFraction);
    double discountFactorDw = DF * Math.exp(EPS * relativeYearFraction);
    double rateUp = (Math.pow(discountFactorUp, -1d / PERIOD_PER_YEAR / relativeYearFraction) - 1d) * PERIOD_PER_YEAR;
    double rateDw = (Math.pow(discountFactorDw, -1d / PERIOD_PER_YEAR / relativeYearFraction) - 1d) * PERIOD_PER_YEAR;
    double expected = 0.5 * NOTIONAL_USD / EPS * (
        discountFactorFromPeriodicallyCompoundedRate(rateUp + Z_SPREAD, PERIOD_PER_YEAR, relativeYearFraction) -
        discountFactorFromPeriodicallyCompoundedRate(rateDw + Z_SPREAD, PERIOD_PER_YEAR, relativeYearFraction));
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertThat(actual.getCurrency()).isEqualTo(USD);
    assertThat(actual.getCurveCurrency()).isEqualTo(USD);
    assertThat(actual.getYearFraction()).isEqualTo(relativeYearFraction);
    assertThat(actual.getSensitivity()).isCloseTo(expected, offset(NOTIONAL_USD * EPS));
  }

  @Test
  public void test_presentValueSensitivityWithSpread_df_spread_ended() {
    PointSensitivities computed =
        PRICER.presentValueSensitivityWithSpread(PAYMENT_PAST, DISCOUNT_FACTORS, Z_SPREAD, PERIODIC, 3).build();
    assertThat(computed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValue_provider() {
    assertThat(PRICER.forecastValue(PAYMENT, PROVIDER).getAmount()).isCloseTo(NOTIONAL_USD, offset(0d));
    assertThat(PRICER.forecastValueAmount(PAYMENT, PROVIDER)).isCloseTo(NOTIONAL_USD, offset(0d));
  }

  @Test
  public void test_forecastValue_provider_ended() {
    assertThat(PRICER.forecastValue(PAYMENT_PAST, PROVIDER).getAmount()).isCloseTo(0d, offset(0d));
    assertThat(PRICER.forecastValueAmount(PAYMENT_PAST, PROVIDER)).isCloseTo(0d, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cashFlow_provider() {
    CashFlow expected = CashFlow.ofForecastValue(PAYMENT_DATE, USD, NOTIONAL_USD, DF);
    assertThat(PRICER.cashFlows(PAYMENT, PROVIDER)).isEqualTo(CashFlows.of(expected));
  }

  @Test
  public void test_cashFlow_provider_ended() {
    assertThat(PRICER.cashFlows(PAYMENT_PAST, PROVIDER)).isEqualTo(CashFlows.NONE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    assertThat(PRICER.currencyExposure(PAYMENT, PROVIDER)).isEqualTo(MultiCurrencyAmount.of(PRICER.presentValue(PAYMENT, PROVIDER)));
  }

  @Test
  public void test_currentCash_onDate() {
    SimpleRatesProvider prov = new SimpleRatesProvider(PAYMENT.getDate(), DISCOUNT_FACTORS);
    assertThat(PRICER.currentCash(PAYMENT, prov)).isEqualTo(PAYMENT.getValue());
  }

  @Test
  public void test_currentCash_past() {
    assertThat(PRICER.currentCash(PAYMENT_PAST, PROVIDER)).isEqualTo(CurrencyAmount.zero(USD));
  }

}
