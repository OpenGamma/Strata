/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
import com.opengamma.strata.pricer.bond.DiscountingCapitalIndexedBondTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBondTrade;

/**
 * Test {@link CapitalIndexedBondTradeCalculations}.
 */
public class CapitalIndexedBondTradeCalculationsTest {

  private static final ResolvedCapitalIndexedBondTrade RTRADE = CapitalIndexedBondTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = CapitalIndexedBondTradeCalculationFunctionTest.RATES_LOOKUP;
  private static final LegalEntityDiscountingMarketDataLookup LED_LOOKUP =
      CapitalIndexedBondTradeCalculationFunctionTest.LED_LOOKUP;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = CapitalIndexedBondTradeCalculationFunctionTest.marketData();
    RatesProvider ratesProvider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    LegalEntityDiscountingProvider ledProvider = LED_LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingCapitalIndexedBondTradePricer pricer = DiscountingCapitalIndexedBondTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, ratesProvider, ledProvider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, ratesProvider, ledProvider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, ratesProvider);

    assertThat(CapitalIndexedBondTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, LED_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(CapitalIndexedBondTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, LED_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertThat(CapitalIndexedBondTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, LED_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  @Test
  public void test_pv01() {
    ScenarioMarketData md = CapitalIndexedBondTradeCalculationFunctionTest.marketData();
    RatesProvider ratesProvider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    LegalEntityDiscountingProvider ledProvider = LED_LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingCapitalIndexedBondTradePricer pricer = DiscountingCapitalIndexedBondTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, ratesProvider, ledProvider);
    CurrencyParameterSensitivities pvParamSens = ledProvider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(CapitalIndexedBondTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, RATES_LOOKUP, LED_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(CapitalIndexedBondTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, RATES_LOOKUP, LED_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
