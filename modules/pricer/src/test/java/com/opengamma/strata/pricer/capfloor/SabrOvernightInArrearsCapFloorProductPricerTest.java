/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.CAP_LEG;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.CAP_ONE_LEG;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.CAP_TWO_LEGS;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.NOTIONAL_VALUE;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.PAY_LEG;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.RATES;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.RATES_PAY;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.VOLS;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.VOLS_PAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;

/**
 * Test {@link SabrOvernightInArrearsCapFloorProductPricer}
 */
public class SabrOvernightInArrearsCapFloorProductPricerTest {

  private static final SabrOvernightInArrearsCapFloorProductPricer PRICER =
      SabrOvernightInArrearsCapFloorProductPricer.DEFAULT;
  private static final SabrOvernightInArrearsCapFloorLegPricer PRICER_CAP_LEG = PRICER.getCapFloorLegPricer();
  private static final DiscountingSwapLegPricer PRICER_PAY_LEG = PRICER.getPayLegPricer();
  private static final double TOL = 1.0e-13;

  @Test
  public void test_presentValue() {
    MultiCurrencyAmount computed1 = PRICER.presentValue(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValue(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValue(CAP_LEG, RATES, VOLS);
    CurrencyAmount pay = PRICER_PAY_LEG.presentValue(PAY_LEG, RATES);
    assertThat(computed1).isEqualTo(MultiCurrencyAmount.of(cap));
    assertThat(computed2).isEqualTo(MultiCurrencyAmount.of(cap.plus(pay)));
  }

  @Test
  public void test_presentValueCapletFloorletPeriods() {
    OvernightInArrearsCapletFloorletPeriodCurrencyAmounts computed1 =
        PRICER.presentValueCapletFloorletPeriods(CAP_ONE_LEG, RATES, VOLS);
    OvernightInArrearsCapletFloorletPeriodCurrencyAmounts computed2 =
        PRICER.presentValueCapletFloorletPeriods(CAP_TWO_LEGS, RATES, VOLS);
    OvernightInArrearsCapletFloorletPeriodCurrencyAmounts expected =
        PRICER_CAP_LEG.presentValueCapletFloorletPeriods(CAP_LEG, RATES, VOLS);
    assertThat(computed1).isEqualTo(expected);
    assertThat(computed2).isEqualTo(expected); // calc ignores pay leg pv
  }

  @Test
  public void test_presentValueSensitivityRatesStickyModel() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivityRatesStickyModel(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivityRatesStickyModel(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivityRatesStickyModel(CAP_LEG, RATES, VOLS);
    PointSensitivityBuilder pay = PRICER_PAY_LEG.presentValueSensitivity(PAY_LEG, RATES);
    assertThat(computed1).isEqualTo(cap);
    assertThat(computed2).isEqualTo(cap.combinedWith(pay));
  }

  @Test
  public void test_presentValueSensitivityModelParamsSabr() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivityModelParamsSabr(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivityModelParamsSabr(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivityModelParamsSabr(CAP_LEG, RATES, VOLS);
    assertThat(computed1).isEqualTo(cap);
    assertThat(computed2).isEqualTo(cap);
  }

  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount computed1 = PRICER.currencyExposure(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.currencyExposure(CAP_TWO_LEGS, RATES, VOLS);
    MultiCurrencyAmount pv1 = PRICER.presentValue(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount pv2 = PRICER.presentValue(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder point1 = PRICER.presentValueSensitivityRatesStickyModel(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder point2 = PRICER.presentValueSensitivityRatesStickyModel(CAP_TWO_LEGS, RATES, VOLS);
    MultiCurrencyAmount expected1 = RATES.currencyExposure(point1.build()).plus(pv1);
    MultiCurrencyAmount expected2 = RATES.currencyExposure(point2.build()).plus(pv2);
    assertThat(computed1.getAmount(EUR).getAmount())
        .isCloseTo(expected1.getAmount(EUR).getAmount(), offset(NOTIONAL_VALUE * TOL));
    assertThat(computed2.getAmount(EUR).getAmount())
        .isCloseTo(expected2.getAmount(EUR).getAmount(), offset(NOTIONAL_VALUE * TOL));
  }

  @Test
  public void test_currentCash() {
    MultiCurrencyAmount cc1 = PRICER.currentCash(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount cc2 = PRICER.currentCash(CAP_TWO_LEGS, RATES, VOLS);
    assertThat(cc1).isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
    assertThat(cc2).isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
  }

  @Test
  public void test_currentCash_onPay() {
    MultiCurrencyAmount cc1 = PRICER.currentCash(CAP_ONE_LEG, RATES_PAY, VOLS_PAY);
    MultiCurrencyAmount cc2 = PRICER.currentCash(CAP_TWO_LEGS, RATES_PAY, VOLS_PAY);
    CurrencyAmount ccCap = PRICER_CAP_LEG.currentCash(CAP_LEG, RATES_PAY, VOLS_PAY);
    CurrencyAmount ccPay = PRICER_PAY_LEG.currentCash(PAY_LEG, RATES_PAY);
    assertThat(cc1).isEqualTo(MultiCurrencyAmount.of(ccCap));
    assertThat(cc2).isEqualTo(MultiCurrencyAmount.of(ccCap).plus(ccPay));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_impliedVolatility() {
    OvernightInArrearsCapletFloorletPeriodAmounts computed = PRICER.impliedVolatilities(CAP_ONE_LEG, RATES, VOLS);
    OvernightInArrearsCapletFloorletPeriodAmounts expected = PRICER_CAP_LEG.impliedVolatilities(CAP_LEG, RATES, VOLS);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_impliedVolatility_onFix() {
    OvernightInArrearsCapletFloorletPeriodAmounts computed = PRICER.impliedVolatilities(
        CAP_ONE_LEG,
        RATES_PAY,
        VOLS_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts expected = PRICER_CAP_LEG.impliedVolatilities(
        CAP_LEG,
        RATES_PAY,
        VOLS_PAY);
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forwardRate() {
    OvernightInArrearsCapletFloorletPeriodAmounts computed = PRICER.forwardRates(CAP_ONE_LEG, RATES);
    OvernightInArrearsCapletFloorletPeriodAmounts expected = PRICER_CAP_LEG.forwardRates(CAP_LEG, RATES);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_forwardRate_onFix() {
    OvernightInArrearsCapletFloorletPeriodAmounts computed = PRICER.forwardRates(CAP_ONE_LEG, RATES_PAY);
    OvernightInArrearsCapletFloorletPeriodAmounts expected = PRICER_CAP_LEG.forwardRates(CAP_LEG, RATES_PAY);
    assertThat(computed).isEqualTo(expected);
  }

}
