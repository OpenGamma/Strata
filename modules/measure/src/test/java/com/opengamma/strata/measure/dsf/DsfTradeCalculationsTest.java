/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.dsf;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.dsf.DiscountingDsfTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.dsf.ResolvedDsfTrade;

/**
 * Test {@link DsfTradeCalculations}.
 */
@Test
public class DsfTradeCalculationsTest {

  private static final ResolvedDsfTrade RTRADE = DsfTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = DsfTradeCalculationFunctionTest.RATES_LOOKUP;
  private static final double REF_PRICE = DsfTradeCalculationFunctionTest.REF_PRICE;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = DsfTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingDsfTradePricer pricer = DiscountingDsfTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, REF_PRICE);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, REF_PRICE);

    AssertJUnit.assertEquals(
        DsfTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, md),
        CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    AssertJUnit.assertEquals(
        DsfTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
  }

  @Test
  public void test_pv01() {
    ScenarioMarketData md = DsfTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingDsfTradePricer pricer = DiscountingDsfTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    AssertJUnit.assertEquals(
        DsfTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, RATES_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    AssertJUnit.assertEquals(
        DsfTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, RATES_LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
