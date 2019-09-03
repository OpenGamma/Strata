/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Test {@link SwapTradeCalculations}.
 */
public class SwapTradeCalculationsTest {

  private static final ResolvedSwapTrade RTRADE = SwapTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = SwapTradeCalculationFunctionTest.RATES_LOOKUP;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = SwapTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingSwapTradePricer pricer = DiscountingSwapTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider);
    ExplainMap expectedExplainPv = pricer.explainPresentValue(RTRADE, provider);
    double expectedParRate = pricer.parRate(RTRADE, provider);
    double expectedParSpread = pricer.parSpread(RTRADE, provider);
    CashFlows expectedCashFlows = pricer.cashFlows(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider);
    MultiCurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider);

    assertThat(SwapTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(SwapTradeCalculations.DEFAULT.explainPresentValue(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedExplainPv)));
    assertThat(SwapTradeCalculations.DEFAULT.parRate(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(DoubleScenarioArray.of(ImmutableList.of(expectedParRate)));
    assertThat(SwapTradeCalculations.DEFAULT.parSpread(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(DoubleScenarioArray.of(ImmutableList.of(expectedParSpread)));
    assertThat(SwapTradeCalculations.DEFAULT.cashFlows(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedCashFlows)));
    assertThat(SwapTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertThat(SwapTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  @Test
  public void test_pv01() {
    ScenarioMarketData md = SwapTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingSwapTradePricer pricer = DiscountingSwapTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(SwapTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(SwapTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
