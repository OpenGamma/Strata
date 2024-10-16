/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.CAP_ONE_LEG;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.CAP_TWO_LEGS;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.NOTIONAL_VALUE;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.PREMIUM;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.RATES;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.RATES_PAY;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.TRADE;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.TRADE_PAYLEG;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.TRADE_PREMIUM;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.VOLS;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.VOLS_PAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;

/**
 * Test {@link SabrOvernightInArrearsCapFloorTradePricer}.
 */
public class SabrOvernightInArrearsCapFloorTradePricerTest {

  private static final SabrOvernightInArrearsCapFloorTradePricer PRICER =
      SabrOvernightInArrearsCapFloorTradePricer.DEFAULT;
  private static final SabrOvernightInArrearsCapFloorProductPricer PRICER_PRODUCT = PRICER.getProductPricer();
  private static final DiscountingPaymentPricer PRICER_PREMIUM = PRICER.getPaymentPricer();
  private static final double TOL = 1.0e-13;

  @Test
  public void test_presentValue() {
    MultiCurrencyAmount computedWithPayLeg = PRICER.presentValue(TRADE_PAYLEG, RATES, VOLS);
    MultiCurrencyAmount computedWithPremium = PRICER.presentValue(TRADE_PREMIUM, RATES, VOLS);
    MultiCurrencyAmount pvOneLeg = PRICER_PRODUCT.presentValue(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount pvTwoLegs = PRICER_PRODUCT.presentValue(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount pvPrem = PRICER_PREMIUM.presentValue(PREMIUM, RATES);
    assertThat(computedWithPayLeg).isEqualTo(pvTwoLegs);
    assertThat(computedWithPremium).isEqualTo(pvOneLeg.plus(pvPrem));
  }

  @Test
  public void test_presentValueCapletFloorletPeriods() {
    OvernightInArrearsCapletFloorletPeriodCurrencyAmounts computed = PRICER.presentValueCapletFloorletPeriods(
        TRADE,
        RATES,
        VOLS);
    OvernightInArrearsCapletFloorletPeriodCurrencyAmounts computedWithPayLeg = PRICER.presentValueCapletFloorletPeriods(
        TRADE_PAYLEG,
        RATES,
        VOLS);
    OvernightInArrearsCapletFloorletPeriodCurrencyAmounts computedWithPremium = PRICER.presentValueCapletFloorletPeriods(
        TRADE_PREMIUM,
        RATES,
        VOLS);
    OvernightInArrearsCapletFloorletPeriodCurrencyAmounts expected = PRICER_PRODUCT.presentValueCapletFloorletPeriods(
        CAP_ONE_LEG,
        RATES,
        VOLS);
    assertThat(computed).isEqualTo(expected);
    assertThat(computedWithPayLeg).isEqualTo(expected); // calc ignores pay leg pv
    assertThat(computedWithPremium).isEqualTo(expected); // calc ignores premium pv
  }

  @Test
  public void test_presentValueSensitivityRatesStickyModel() {
    PointSensitivities computedWithPayLeg = PRICER.presentValueSensitivityRatesStickyModel(TRADE_PAYLEG, RATES, VOLS);
    PointSensitivities computedWithPremium = PRICER.presentValueSensitivityRatesStickyModel(TRADE_PREMIUM, RATES, VOLS);
    PointSensitivities pvOneLeg = PRICER_PRODUCT.presentValueSensitivityRatesStickyModel(
        CAP_ONE_LEG,
        RATES,
        VOLS).build();
    PointSensitivities pvTwoLegs = PRICER_PRODUCT.presentValueSensitivityRatesStickyModel(
        CAP_TWO_LEGS,
        RATES,
        VOLS).build();
    PointSensitivities pvPrem = PRICER_PREMIUM.presentValueSensitivity(PREMIUM, RATES).build();
    assertThat(computedWithPayLeg).isEqualTo(pvTwoLegs);
    assertThat(computedWithPremium).isEqualTo(pvOneLeg.combinedWith(pvPrem));
  }

  @Test
  public void test_presentValueSensitivityModelParamsSabr() {
    PointSensitivities computedWithPayLeg = PRICER.presentValueSensitivityModelParamsSabr(
        TRADE_PAYLEG,
        RATES,
        VOLS).build();
    PointSensitivities computedWithPremium = PRICER.presentValueSensitivityModelParamsSabr(
        TRADE_PREMIUM,
        RATES,
        VOLS).build();
    PointSensitivities pvOneLeg = PRICER_PRODUCT.presentValueSensitivityModelParamsSabr(
        CAP_ONE_LEG,
        RATES,
        VOLS).build();
    PointSensitivities pvTwoLegs = PRICER_PRODUCT.presentValueSensitivityModelParamsSabr(
        CAP_TWO_LEGS,
        RATES,
        VOLS).build();
    assertThat(computedWithPayLeg).isEqualTo(pvTwoLegs);
    assertThat(computedWithPremium).isEqualTo(pvOneLeg);
  }

  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount computedWithPayLeg = PRICER.currencyExposure(TRADE_PAYLEG, RATES, VOLS);
    MultiCurrencyAmount computedWithPremium = PRICER.currencyExposure(TRADE_PREMIUM, RATES, VOLS);
    MultiCurrencyAmount pvWithPayLeg = PRICER.presentValue(TRADE_PAYLEG, RATES, VOLS);
    MultiCurrencyAmount pvWithPremium = PRICER.presentValue(TRADE_PREMIUM, RATES, VOLS);
    PointSensitivities pointWithPayLeg = PRICER.presentValueSensitivityRatesStickyModel(TRADE_PAYLEG, RATES, VOLS);
    PointSensitivities pointWithPremium = PRICER.presentValueSensitivityRatesStickyModel(TRADE_PREMIUM, RATES, VOLS);
    MultiCurrencyAmount expectedWithPayLeg = RATES.currencyExposure(pointWithPayLeg).plus(pvWithPayLeg);
    MultiCurrencyAmount expectedWithPremium = RATES.currencyExposure(pointWithPremium).plus(pvWithPremium);
    assertThat(computedWithPayLeg.getAmount(EUR).getAmount()).isCloseTo(
        expectedWithPayLeg.getAmount(EUR).getAmount(),
        offset(NOTIONAL_VALUE * TOL));
    assertThat(computedWithPremium.getAmount(EUR).getAmount()).isCloseTo(
        expectedWithPremium.getAmount(EUR).getAmount(),
        offset(NOTIONAL_VALUE * TOL));
  }

  @Test
  public void test_currentCash() {
    MultiCurrencyAmount computedWithPayLeg = PRICER.currentCash(TRADE_PAYLEG, RATES, VOLS);
    MultiCurrencyAmount computedWithPremium = PRICER.currentCash(TRADE_PREMIUM, RATES, VOLS);
    assertThat(computedWithPayLeg).isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
    assertThat(computedWithPremium).isEqualTo(MultiCurrencyAmount.of(PREMIUM.getValue()));
  }

  @Test
  public void test_currentCash_onPay() {
    MultiCurrencyAmount computedWithPayLeg = PRICER.currentCash(TRADE_PAYLEG, RATES_PAY, VOLS_PAY);
    MultiCurrencyAmount computedWithPremium = PRICER.currentCash(TRADE_PREMIUM, RATES_PAY, VOLS_PAY);
    MultiCurrencyAmount expectedWithPayLeg = PRICER_PRODUCT.currentCash(CAP_TWO_LEGS, RATES_PAY, VOLS_PAY);
    MultiCurrencyAmount expectedWithPremium = PRICER_PRODUCT.currentCash(CAP_ONE_LEG, RATES_PAY, VOLS_PAY);
    assertThat(computedWithPayLeg).isEqualTo(expectedWithPayLeg);
    assertThat(computedWithPremium).isEqualTo(expectedWithPremium);
  }

  @Test
  public void test_forwardRates() {
    OvernightInArrearsCapletFloorletPeriodAmounts computedWithPayLeg = PRICER.forwardRates(TRADE_PAYLEG, RATES_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts computedWithPremium = PRICER.forwardRates(TRADE_PREMIUM, RATES_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts expectedWithPayLeg = PRICER_PRODUCT.forwardRates(
        CAP_TWO_LEGS,
        RATES_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts expectedWithPremium = PRICER_PRODUCT.forwardRates(
        CAP_ONE_LEG,
        RATES_PAY);
    assertThat(computedWithPayLeg).isEqualTo(expectedWithPayLeg);
    assertThat(computedWithPremium).isEqualTo(expectedWithPremium);
  }

  @Test
  public void test_impliedVolatilities() {
    OvernightInArrearsCapletFloorletPeriodAmounts computedWithPayLeg = PRICER.impliedVolatilities(
        TRADE_PAYLEG,
        RATES_PAY,
        VOLS_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts computedWithPremium = PRICER.impliedVolatilities(
        TRADE_PREMIUM,
        RATES_PAY,
        VOLS_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts expectedWithPayLeg = PRICER_PRODUCT.impliedVolatilities(
        CAP_TWO_LEGS,
        RATES_PAY,
        VOLS_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts expectedWithPremium = PRICER_PRODUCT.impliedVolatilities(
        CAP_ONE_LEG,
        RATES_PAY,
        VOLS_PAY);
    assertThat(computedWithPayLeg).isEqualTo(expectedWithPayLeg);
    assertThat(computedWithPremium).isEqualTo(expectedWithPremium);
  }

}
