/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.pricer.bond.DiscountingBillTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.bond.ResolvedBillTrade;

/**
 * Test {@link BillTradeCalculations}.
 */
public class BillTradeCalculationsTest {

  private static final ResolvedBillTrade RTRADE = BillTradeCalculationFunctionTest.RTRADE;
  private static final LegalEntityDiscountingMarketDataLookup LOOKUP = BillTradeCalculationFunctionTest.LOOKUP;
  private static final BillTradeCalculations CALC = BillTradeCalculations.DEFAULT;
  private static final DiscountingBillTradePricer PRICER = DiscountingBillTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQ_CALC = MarketQuoteSensitivityCalculator.DEFAULT;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = BillTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    CurrencyAmount expectedPv = PRICER.presentValue(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = PRICER.currencyExposure(RTRADE, provider);
    CurrencyAmount expectedCurrentCash = PRICER.currentCash(RTRADE, provider.getValuationDate());

    assertThat(CALC.presentValue(RTRADE, LOOKUP, md)).isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(CALC.currencyExposure(RTRADE, LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertThat(CALC.currentCash(RTRADE, LOOKUP, md)).isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
    assertThat(CALC.presentValue(RTRADE, provider)).isEqualTo(expectedPv);
    assertThat(CALC.currencyExposure(RTRADE, provider)).isEqualTo(expectedCurrencyExposure);
    assertThat(CALC.currentCash(RTRADE, provider)).isEqualTo(expectedCurrentCash);
  }

  @Test
  public void test_pv01_calibrated() {
    ScenarioMarketData md = BillTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    PointSensitivities pvPointSens = PRICER.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(BillTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(BillTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
    assertThat(BillTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, provider)).isEqualTo(expectedPv01Cal);
    assertThat(BillTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, provider)).isEqualTo(expectedPv01CalBucketed);
  }

  @Test
  public void test_pv01_quote() {
    ScenarioMarketData md = BillTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    PointSensitivities pvPointSens = PRICER.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    CurrencyParameterSensitivities expectedPv01CalBucketed = MQ_CALC.sensitivity(pvParamSens, provider).multipliedBy(1e-4);
    MultiCurrencyAmount expectedPv01Cal = expectedPv01CalBucketed.total();

    assertThat(BillTradeCalculations.DEFAULT.pv01MarketQuoteSum(RTRADE, LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(BillTradeCalculations.DEFAULT.pv01MarketQuoteBucketed(RTRADE, LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
    assertThat(BillTradeCalculations.DEFAULT.pv01MarketQuoteSum(RTRADE, provider)).isEqualTo(expectedPv01Cal);
    assertThat(BillTradeCalculations.DEFAULT.pv01MarketQuoteBucketed(RTRADE, provider)).isEqualTo(expectedPv01CalBucketed);
  }

}
