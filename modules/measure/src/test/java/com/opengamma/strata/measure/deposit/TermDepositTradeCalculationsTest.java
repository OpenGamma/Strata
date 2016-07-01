/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.deposit;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;

/**
 * Test {@link TermDepositTradeCalculations}.
 */
@Test
public class TermDepositTradeCalculationsTest {

  private static final ResolvedTermDepositTrade RTRADE = TermDepositTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = TermDepositTradeCalculationFunctionTest.RATES_LOOKUP;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = TermDepositTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingTermDepositTradePricer pricer = DiscountingTermDepositTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider);
    double expectedParRate = pricer.parRate(RTRADE, provider);
    double expectedParSpread = pricer.parSpread(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider);

    assertEquals(
        TermDepositTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, md),
        CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        TermDepositTradeCalculations.DEFAULT.parRate(RTRADE, RATES_LOOKUP, md),
        DoubleScenarioArray.of(ImmutableList.of(expectedParRate)));
    assertEquals(
        TermDepositTradeCalculations.DEFAULT.parSpread(RTRADE, RATES_LOOKUP, md),
        DoubleScenarioArray.of(ImmutableList.of(expectedParSpread)));
    assertEquals(
        TermDepositTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        TermDepositTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, md),
        CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  public void test_pv01() {
    ScenarioMarketData md = TermDepositTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingTermDepositTradePricer pricer = DiscountingTermDepositTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        TermDepositTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        TermDepositTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, RATES_LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
