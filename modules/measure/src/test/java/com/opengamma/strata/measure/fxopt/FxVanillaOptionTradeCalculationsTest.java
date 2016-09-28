/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.measure.fxopt.FxVanillaOptionMethod.BLACK;
import static org.testng.Assert.assertEquals;

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
import com.opengamma.strata.pricer.fxopt.BlackFxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxVanillaOptionTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOptionTrade;

/**
 * Test {@link FxVanillaOptionTradeCalculations}.
 */
@Test
public class FxVanillaOptionTradeCalculationsTest {

  private static final ResolvedFxVanillaOptionTrade RTRADE = FxVanillaOptionTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = FxVanillaOptionTradeCalculationFunctionTest.RATES_LOOKUP;
  private static final FxOptionMarketDataLookup FX_OPTION_LOOKUP = FxVanillaOptionTradeCalculationFunctionTest.FX_OPTION_LOOKUP;
  private static final BlackFxOptionVolatilities VOLS = FxVanillaOptionTradeCalculationFunctionTest.VOLS;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = FxVanillaOptionTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    BlackFxVanillaOptionTradePricer pricer = BlackFxVanillaOptionTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, VOLS);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider.getValuationDate());

    assertEquals(
        FxVanillaOptionTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        FxVanillaOptionTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        FxVanillaOptionTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  public void test_pv01() {
    ScenarioMarketData md = FxVanillaOptionTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    BlackFxVanillaOptionTradePricer pricer = BlackFxVanillaOptionTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivityRatesStickyStrike(RTRADE, provider, VOLS);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        FxVanillaOptionTradeCalculations.DEFAULT.pv01RatesCalibratedSum(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        FxVanillaOptionTradeCalculations.DEFAULT.pv01RatesCalibratedBucketed(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
