/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

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
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionTradePricer;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;

/**
 * Test {@link SwaptionTradeCalculations}.
 */
public class SwaptionTradeCalculationsTest {

  private static final ResolvedSwaptionTrade RTRADE = SwaptionTradeCalculationFunctionTest.RTRADE;
  private static final RatesMarketDataLookup RATES_LOOKUP = SwaptionTradeCalculationFunctionTest.RATES_LOOKUP;
  private static final SwaptionMarketDataLookup SWAPTION_LOOKUP = SwaptionTradeCalculationFunctionTest.SWAPTION_LOOKUP;
  private static final SwaptionVolatilities VOLS =
      SwaptionTradeCalculationFunctionTest.NORMAL_VOL_SWAPTION_PROVIDER_USD;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = SwaptionTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    VolatilitySwaptionTradePricer pricer = VolatilitySwaptionTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, VOLS);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider.getValuationDate());

    assertThat(SwaptionTradeCalculations.DEFAULT.presentValue(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(SwaptionTradeCalculations.DEFAULT.currencyExposure(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertThat(SwaptionTradeCalculations.DEFAULT.currentCash(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  @Test
  public void test_pv01() {
    ScenarioMarketData md = SwaptionTradeCalculationFunctionTest.marketData();
    RatesProvider provider = RATES_LOOKUP.marketDataView(md.scenario(0)).ratesProvider();
    VolatilitySwaptionTradePricer pricer = VolatilitySwaptionTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivityRatesStickyStrike(RTRADE, provider, VOLS);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(SwaptionTradeCalculations.DEFAULT.pv01RatesCalibratedSum(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(SwaptionTradeCalculations.DEFAULT.pv01RatesCalibratedBucketed(RTRADE, RATES_LOOKUP, SWAPTION_LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
