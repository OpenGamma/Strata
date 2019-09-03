/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.DiscountingFixedCouponBondTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondTrade;

/**
 * Test {@link FixedCouponBondTradeCalculations}.
 */
public class FixedCouponBondTradeCalculationsTest {

  private static final ResolvedFixedCouponBondTrade RTRADE = FixedCouponBondTradeCalculationFunctionTest.RTRADE;
  private static final LegalEntityDiscountingMarketDataLookup LOOKUP = FixedCouponBondTradeCalculationFunctionTest.LOOKUP;
  private static final MarketQuoteSensitivityCalculator MQ_CALC = MarketQuoteSensitivityCalculator.DEFAULT;

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    ScenarioMarketData md = FixedCouponBondTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingFixedCouponBondTradePricer pricer = DiscountingFixedCouponBondTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider.getValuationDate());

    assertThat(FixedCouponBondTradeCalculations.DEFAULT.presentValue(RTRADE, LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertThat(FixedCouponBondTradeCalculations.DEFAULT.currencyExposure(RTRADE, LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertThat(FixedCouponBondTradeCalculations.DEFAULT.currentCash(RTRADE, LOOKUP, md))
        .isEqualTo(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
  }

  @Test
  public void test_pv01_calibrated() {
    ScenarioMarketData md = FixedCouponBondTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingFixedCouponBondTradePricer pricer = DiscountingFixedCouponBondTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertThat(FixedCouponBondTradeCalculations.DEFAULT.pv01CalibratedSum(RTRADE, LOOKUP, md))
        .isEqualTo(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertThat(FixedCouponBondTradeCalculations.DEFAULT.pv01CalibratedBucketed(RTRADE, LOOKUP, md))
        .isEqualTo(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

  @Test
  public void test_pv01_quote() {
    ScenarioMarketData md = FixedCouponBondTradeCalculationFunctionTest.marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingFixedCouponBondTradePricer pricer = DiscountingFixedCouponBondTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    CurrencyParameterSensitivities expectedPv01CalBucketed = MQ_CALC.sensitivity(pvParamSens, provider).multipliedBy(1e-4);
    MultiCurrencyAmount expectedPv01Cal = expectedPv01CalBucketed.total();

    MultiCurrencyScenarioArray sumComputed = FixedCouponBondTradeCalculations.DEFAULT.pv01MarketQuoteSum(RTRADE, LOOKUP, md);
    ScenarioArray<CurrencyParameterSensitivities> bucketedComputed =
        FixedCouponBondTradeCalculations.DEFAULT.pv01MarketQuoteBucketed(RTRADE, LOOKUP, md);
    assertThat(sumComputed.getScenarioCount()).isEqualTo(1);
    assertThat(sumComputed.get(0).getCurrencies()).containsOnly(GBP);
    assertThat(DoubleMath.fuzzyEquals(
        sumComputed.get(0).getAmount(GBP).getAmount(),
        expectedPv01Cal.getAmount(GBP).getAmount(),
        1.0e-10)).isTrue();
    assertThat(bucketedComputed.getScenarioCount()).isEqualTo(1);
    assertThat(bucketedComputed.get(0).equalWithTolerance(expectedPv01CalBucketed, 1.0e-10)).isTrue();
  }

}
