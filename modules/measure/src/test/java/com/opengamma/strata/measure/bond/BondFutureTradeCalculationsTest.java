/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
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
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Test {@link BondFutureTradeCalculations}.
 */
public class BondFutureTradeCalculationsTest {

  private static final ResolvedBondFutureTrade RTRADE = BondFutureTradeCalculationFunctionTest.RTRADE;
  private static final LegalEntityDiscountingMarketDataLookup LOOKUP = BondFutureTradeCalculationFunctionTest.LOOKUP;
  private static final double SETTLE_PRICE = BondFutureTradeCalculationFunctionTest.SETTLE_PRICE;
  private static final MarketQuoteSensitivityCalculator MQ_CALC = MarketQuoteSensitivityCalculator.DEFAULT;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = BondFutureTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, SETTLE_PRICE);
    double expectedParSpread = pricer.parSpread(RTRADE, provider, SETTLE_PRICE);

    assertThat(BondFutureTradeCalculations.DEFAULT.presentValue(RTRADE, LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(BondFutureTradeCalculations.DEFAULT.parSpread(RTRADE, LOOKUP, md))
        .isEqualTo(DoubleScenarioArray.of(ImmutableList.of(expectedParSpread)));
  }

  @Test
  public void test_pv01_calibrated() {
    ScenarioMarketData md = BondFutureTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(BondFutureTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(BondFutureTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

  @Test
  public void test_pv01_quote() {
    ScenarioMarketData md = BondFutureTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    CurrencyParameterSensitivities expectedPv01CalBucketed = MQ_CALC.sensitivity(pvParamSens.multipliedBy(1e-4), provider);
    MultiCurrencyAmount expectedPv01Cal = expectedPv01CalBucketed.total();

    MultiCurrencyScenarioArray sumComputed = BondFutureTradeCalculations.DEFAULT.pv01MarketQuoteSum(RTRADE, LOOKUP, md);
    ScenarioArray<CurrencyParameterSensitivities> bucketedComputed =
        BondFutureTradeCalculations.DEFAULT.pv01MarketQuoteBucketed(RTRADE, LOOKUP, md);
    assertThat(sumComputed.getScenarioCount()).isEqualTo(1);
    assertThat(sumComputed.get(0).getCurrencies()).containsOnly(USD);
    assertThat(DoubleMath.fuzzyEquals(
        sumComputed.get(0).getAmount(USD).getAmount(),
        expectedPv01Cal.getAmount(USD).getAmount(),
        1.0e-10)).isTrue();
    assertThat(bucketedComputed.getScenarioCount()).isEqualTo(1);
    assertThat(bucketedComputed.get(0).equalWithTolerance(expectedPv01CalBucketed, 1.0e-10)).isTrue();
  }

}
