/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

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
import com.opengamma.strata.pricer.bond.DiscountingBillTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.ResolvedBillTrade;

/**
 * Test {@link BillTradeCalculations}.
 */
@Test
public class BillTradeCalculationsTest {

  private static final ResolvedBillTrade RTRADE = BillTradeCalculationFunctionTest.RTRADE;
  private static final LegalEntityDiscountingMarketDataLookup LOOKUP = BillTradeCalculationFunctionTest.LOOKUP;
  private static final BillTradeCalculations CALC = BillTradeCalculations.DEFAULT;
  private static final DiscountingBillTradePricer PRICER = DiscountingBillTradePricer.DEFAULT;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = BillTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    CurrencyAmount expectedPv = PRICER.presentValue(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = PRICER.currencyExposure(RTRADE, provider);
    CurrencyAmount expectedCurrentCash = PRICER.currentCash(RTRADE, provider.getValuationDate());

    assertEquals(
        CALC.presentValue(RTRADE, LOOKUP, md),
        CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        CALC.currencyExposure(RTRADE, LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        CALC.currentCash(RTRADE, LOOKUP, md),
        CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
    assertEquals(CALC.presentValue(RTRADE, provider), expectedPv);
    assertEquals(CALC.currencyExposure(RTRADE, provider), expectedCurrencyExposure);
    assertEquals(CALC.currentCash(RTRADE, provider), expectedCurrentCash);
  }

  public void test_pv01() {
    ScenarioMarketData md = BillTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    PointSensitivities pvPointSens = PRICER.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        BillTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        BillTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
    assertEquals(BillTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, provider), expectedPv01Cal);
    assertEquals(BillTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, provider), expectedPv01CalBucketed);
  }

}
