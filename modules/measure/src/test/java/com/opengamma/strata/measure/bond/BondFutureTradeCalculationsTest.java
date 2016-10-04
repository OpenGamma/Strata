/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.DiscountingBondFutureTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Test {@link BondFutureTradeCalculations}.
 */
@Test
public class BondFutureTradeCalculationsTest {

  private static final ResolvedBondFutureTrade RTRADE = BondFutureTradeCalculationFunctionTest.RTRADE;
  private static final LegalEntityDiscountingMarketDataLookup LOOKUP = BondFutureTradeCalculationFunctionTest.LOOKUP;
  private static final double SETTLE_PRICE = BondFutureTradeCalculationFunctionTest.SETTLE_PRICE;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = BondFutureTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, SETTLE_PRICE);
    double expectedParSpread = pricer.parSpread(RTRADE, provider, SETTLE_PRICE);

    assertEquals(
        BondFutureTradeCalculations.DEFAULT.presentValue(RTRADE, LOOKUP, md),
        CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        BondFutureTradeCalculations.DEFAULT.parSpread(RTRADE, LOOKUP, md),
        DoubleScenarioArray.of(ImmutableList.of(expectedParSpread)));
  }

  public void test_pv01() {
    ScenarioMarketData md = BondFutureTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        BondFutureTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, LOOKUP, md),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        BondFutureTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, LOOKUP, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

}
