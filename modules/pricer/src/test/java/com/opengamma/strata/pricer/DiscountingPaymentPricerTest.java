/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.market.value.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.market.value.CompoundedRateType.PERIODIC;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;

/**
 * Test {@link DiscountingPaymentPricer}.
 */
@Test
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

  private static final ConstantNodalCurve CURVE = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_365F), DF);
  private static final SimpleDiscountFactors DISCOUNT_FACTORS = SimpleDiscountFactors.of(USD, VAL_DATE_2014_01_22, CURVE);
  private static final BaseProvider PROVIDER = new SimpleRatesProvider(VAL_DATE_2014_01_22, DISCOUNT_FACTORS);
  private static final double Z_SPREAD = 0.02;
  private static final int PERIOD_PER_YEAR = 4;
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;

  //-------------------------------------------------------------------------
  public void test_presentValue_df() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT, DISCOUNT_FACTORS);
    double expected = NOTIONAL_USD * DF;
    assertEquals(computed.getAmount(), expected, NOTIONAL_USD * TOL);
  }

  public void test_presentValue_df_ended() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT_PAST, DISCOUNT_FACTORS);
    assertEquals(computed, CurrencyAmount.zero(USD));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_provider() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT, PROVIDER);
    double expected = NOTIONAL_USD * DF;
    assertEquals(computed.getAmount(), expected, NOTIONAL_USD * TOL);
  }

  public void test_presentValue_provider_ended() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT_PAST, PROVIDER);
    assertEquals(computed, CurrencyAmount.zero(USD));
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_df_spread_continuous() {
    CurrencyAmount computed = PRICER
        .presentValue(PAYMENT, DISCOUNT_FACTORS, Z_SPREAD, CONTINUOUS, 0);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = NOTIONAL_USD * DF * Math.exp(-Z_SPREAD * relativeYearFraction);
    assertEquals(computed.getAmount(), expected, NOTIONAL_USD * TOL);
  }

  public void test_presentValue_df_spread_periodic() {
    CurrencyAmount computed = PRICER.presentValue(
        PAYMENT, DISCOUNT_FACTORS, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double rate = (Math.pow(DF, -1d / PERIOD_PER_YEAR / relativeYearFraction) - 1d) * PERIOD_PER_YEAR;
    double expected = NOTIONAL_USD *
        discountFactorFromPeriodicallyCompoundedRate(rate + Z_SPREAD, PERIOD_PER_YEAR, relativeYearFraction);
    assertEquals(computed.getAmount(), expected, NOTIONAL_USD * TOL);
  }

  public void test_presentValue_df_ended_spread() {
    CurrencyAmount computed = PRICER.presentValue(PAYMENT_PAST, DISCOUNT_FACTORS, Z_SPREAD, PERIODIC, 3);
    assertEquals(computed, CurrencyAmount.zero(USD));
  }

  private double discountFactorFromPeriodicallyCompoundedRate(double rate, double periodPerYear, double time) {
    return Math.pow(1d + rate / periodPerYear, -periodPerYear * time);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_df() {
    PointSensitivities point = PRICER.presentValueSensitivity(PAYMENT, DISCOUNT_FACTORS).build();
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = -DF * relativeYearFraction * NOTIONAL_USD;
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertEquals(actual.getCurrency(), USD);
    assertEquals(actual.getCurveCurrency(), USD);
    assertEquals(actual.getDate(), PAYMENT_DATE);
    assertEquals(actual.getSensitivity(), expected, NOTIONAL_USD * TOL);
  }

  public void test_presentValueSensitivity_df_ended() {
    PointSensitivities computed = PRICER.presentValueSensitivity(PAYMENT_PAST, DISCOUNT_FACTORS).build();
    assertEquals(computed, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_provider() {
    PointSensitivities point = PRICER.presentValueSensitivity(PAYMENT, PROVIDER).build();
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = -DF * relativeYearFraction * NOTIONAL_USD;
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertEquals(actual.getCurrency(), USD);
    assertEquals(actual.getCurveCurrency(), USD);
    assertEquals(actual.getDate(), PAYMENT_DATE);
    assertEquals(actual.getSensitivity(), expected, NOTIONAL_USD * TOL);
  }

  public void test_presentValueSensitivity_provider_ended() {
    PointSensitivities computed = PRICER.presentValueSensitivity(PAYMENT_PAST, PROVIDER).build();
    assertEquals(computed, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_df_spread_continuous() {
    PointSensitivities point = PRICER.presentValueSensitivity(
        PAYMENT, DISCOUNT_FACTORS, Z_SPREAD, CONTINUOUS, 0).build();
    double relativeYearFraction = ACT_365F.relativeYearFraction(VAL_DATE_2014_01_22, PAYMENT_DATE);
    double expected = -DF * relativeYearFraction * NOTIONAL_USD * Math.exp(-Z_SPREAD * relativeYearFraction);
    ZeroRateSensitivity actual = (ZeroRateSensitivity) point.getSensitivities().get(0);
    assertEquals(actual.getCurrency(), USD);
    assertEquals(actual.getCurveCurrency(), USD);
    assertEquals(actual.getDate(), PAYMENT_DATE);
    assertEquals(actual.getSensitivity(), expected, NOTIONAL_USD * TOL);
  }

  public void test_presentValueSensitivity_df_spread_periodic() {
    PointSensitivities point = PRICER.presentValueSensitivity(
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
    assertEquals(actual.getCurrency(), USD);
    assertEquals(actual.getCurveCurrency(), USD);
    assertEquals(actual.getDate(), PAYMENT_DATE);
    assertEquals(actual.getSensitivity(), expected, NOTIONAL_USD * EPS);
  }

  public void test_presentValueSensitivity_df_spread_ended() {
    PointSensitivities computed =
        PRICER.presentValueSensitivity(PAYMENT_PAST, DISCOUNT_FACTORS, Z_SPREAD, PERIODIC, 3).build();
    assertEquals(computed, PointSensitivities.empty());
  }

}
