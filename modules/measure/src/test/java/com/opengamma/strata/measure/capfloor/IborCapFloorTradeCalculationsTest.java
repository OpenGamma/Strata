/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.capfloor;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.capfloor.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.capfloor.VolatilityIborCapFloorTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorTrade;

/**
 * Test {@link IborCapFloorTradeCalculations}.
 */
@Test
public class IborCapFloorTradeCalculationsTest {

  private static final ResolvedIborCapFloorTrade RTRADE = IborCapFloorTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = IborCapFloorTradeCalculationFunctionTest.RATES_LOOKUP;
  private static final IborCapFloorMarketDataLookup SWAPTION_LOOKUP = IborCapFloorTradeCalculationFunctionTest.SWAPTION_LOOKUP;
  private static final IborCapletFloorletVolatilities VOLS = IborCapFloorTradeCalculationFunctionTest.VOLS;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = IborCapFloorTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    VolatilityIborCapFloorTradePricer pricer = VolatilityIborCapFloorTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider, VOLS);

    assertEquals(
        IborCapFloorTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        IborCapFloorTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        IborCapFloorTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  public void test_pv01() {
    ScenarioMarketData md = IborCapFloorTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    VolatilityIborCapFloorTradePricer pricer = VolatilityIborCapFloorTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivityRates(RTRADE, provider, VOLS);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        IborCapFloorTradeCalculations.DEFAULT.pv01RatesCalibratedSum(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        IborCapFloorTradeCalculations.DEFAULT.pv01RatesCalibratedBucketed(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
