/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Test {@link BulletPaymentTradeCalculations}.
 */
public class BulletPaymentTradeCalculationsTest {

  private static final ResolvedBulletPaymentTrade RTRADE = BulletPaymentTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = BulletPaymentTradeCalculationFunctionTest.RATES_LOOKUP;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = BulletPaymentTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    Payment payment = RTRADE.getProduct().getPayment();
    CurrencyAmount expectedPv = pricer.presentValue(payment, provider);
    CashFlows expectedCashFlows = pricer.cashFlows(payment, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(payment, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(payment, provider);

    assertThat(BulletPaymentTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(BulletPaymentTradeCalculations.DEFAULT.cashFlows(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedCashFlows)));
    assertThat(BulletPaymentTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertThat(BulletPaymentTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  @Test
  public void test_pv01() {
    ScenarioMarketData md = BulletPaymentTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    Payment payment = RTRADE.getProduct().getPayment();
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(payment, provider).build();
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(BulletPaymentTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(BulletPaymentTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
