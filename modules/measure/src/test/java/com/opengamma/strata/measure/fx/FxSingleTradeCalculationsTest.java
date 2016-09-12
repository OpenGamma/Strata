/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingleTrade;

/**
 * Test {@link FxSingleTradeCalculations}.
 */
@Test
public class FxSingleTradeCalculationsTest {

  private static final ResolvedFxSingleTrade RTRADE = FxSingleTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = FxSingleTradeCalculationFunctionTest.RATES_LOOKUP;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = FxSingleTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFxSingleTradePricer pricer = DiscountingFxSingleTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider);
    MultiCurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider);
    FxRate expectedForwardFx = pricer.forwardFxRate(RTRADE, provider);

    assertEquals(
        FxSingleTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        FxSingleTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        FxSingleTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
    assertEquals(
        FxSingleTradeCalculations.DEFAULT.forwardFxRate(RTRADE, RATES_LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedForwardFx)));
  }

  public void test_pv01() {
    ScenarioMarketData md = FxSingleTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFxSingleTradePricer pricer = DiscountingFxSingleTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        FxSingleTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        FxSingleTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, RATES_LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
