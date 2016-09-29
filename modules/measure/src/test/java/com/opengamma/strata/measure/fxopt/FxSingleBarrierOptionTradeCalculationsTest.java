/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.measure.fxopt.FxSingleBarrierOptionMethod.BLACK;
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
import com.opengamma.strata.pricer.fxopt.BlackFxSingleBarrierOptionTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOptionTrade;

/**
 * Test {@link FxSingleBarrierOptionTradeCalculations}.
 */
@Test
public class FxSingleBarrierOptionTradeCalculationsTest {

  private static final ResolvedFxSingleBarrierOptionTrade RTRADE = FxSingleBarrierOptionTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = FxSingleBarrierOptionTradeCalculationFunctionTest.RATES_LOOKUP;
  private static final FxOptionMarketDataLookup FX_OPTION_LOOKUP =
      FxSingleBarrierOptionTradeCalculationFunctionTest.FX_OPTION_LOOKUP;
  private static final BlackFxOptionVolatilities VOLS = FxSingleBarrierOptionTradeCalculationFunctionTest.VOLS;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = FxSingleBarrierOptionTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    BlackFxSingleBarrierOptionTradePricer pricer = BlackFxSingleBarrierOptionTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, VOLS);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider.getValuationDate());

    assertEquals(
        FxSingleBarrierOptionTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        FxSingleBarrierOptionTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        FxSingleBarrierOptionTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  public void test_pv01() {
    ScenarioMarketData md = FxSingleBarrierOptionTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    BlackFxSingleBarrierOptionTradePricer pricer = BlackFxSingleBarrierOptionTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivityRatesStickyStrike(RTRADE, provider, VOLS);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        FxSingleBarrierOptionTradeCalculations.DEFAULT.pv01RatesCalibratedSum(
            RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        FxSingleBarrierOptionTradeCalculations.DEFAULT.pv01RatesCalibratedBucketed(
            RTRADE, RATES_LOOKUP, FX_OPTION_LOOKUP, md, BLACK),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
