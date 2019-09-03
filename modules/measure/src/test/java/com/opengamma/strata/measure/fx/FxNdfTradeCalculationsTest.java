/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.fx.DiscountingFxNdfTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxNdfTrade;

/**
 * Test {@link FxNdfTradeCalculations}.
 */
public class FxNdfTradeCalculationsTest {

  private static final ResolvedFxNdfTrade RTRADE = FxNdfTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = FxNdfTradeCalculationFunctionTest.RATES_LOOKUP;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = FxNdfTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFxNdfTradePricer pricer = DiscountingFxNdfTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider);
    FxRate expectedForwardFx = pricer.forwardFxRate(RTRADE, provider);

    assertThat(FxNdfTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(FxNdfTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertThat(FxNdfTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
    assertThat(FxNdfTradeCalculations.DEFAULT.forwardFxRate(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedForwardFx)));
  }

  @Test
  public void test_pv01() {
    ScenarioMarketData md = FxNdfTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFxNdfTradePricer pricer = DiscountingFxNdfTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(FxNdfTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(FxNdfTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, RATES_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
