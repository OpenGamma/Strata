/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.collect.CollectProjectAssertions;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IborIndexRatesKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.pricer.swap.SwapDummyData;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link SwapFunctionGroups}.
 */
@Test
public class SwapFunctionGroupsTest {

  public static final SwapTrade TRADE = SwapDummyData.SWAP_TRADE;

  public void test_discounting() {
    FunctionGroup<SwapTrade> test = SwapFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PAR_RATE,
        Measure.PRESENT_VALUE,
        Measure.EXPLAIN_PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01,
        Measure.BUCKETED_GAMMA_PV01,
        Measure.LEG_INITIAL_NOTIONAL,
        Measure.LEG_PRESENT_VALUE,
        Measure.ACCRUED_INTEREST);
  }

  public void test_presentValue() {
    Currency ccy = TRADE.getProduct().getLegs().get(0).getCurrency();
    IborIndex index = (IborIndex) TRADE.getProduct().allIndices().iterator().next();
    LocalDate valDate = TRADE.getProduct().getEndDate().plusDays(7);

    FunctionConfig<SwapTrade> config = SwapFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    CalculationSingleFunction<SwapTrade, ?> function = config.createFunction();
    FunctionRequirements reqs = function.requirements(TRADE);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(IborIndexRatesKey.of(index), DiscountFactorsKey.of(ccy)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(index)));
    CollectProjectAssertions.assertThat(function.defaultReportingCurrency(TRADE)).hasValue(ccy);
    DiscountFactors df = SimpleDiscountFactors.of(
        ccy, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    TestMarketDataMap md = new TestMarketDataMap(valDate, ImmutableMap.of(DiscountFactorsKey.of(ccy), df), ImmutableMap.of());
    assertThat(function.execute(TRADE, md)).isEqualTo(MultiCurrencyValuesArray.of(MultiCurrencyAmount.of(ccy, 0d)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(SwapFunctionGroups.class);
  }

  public void coverage_functions() {
    SwapTrade trade = SwapTrade.builder().product(Swap.of(SwapDummyData.FIXED_RATECALC_SWAP_LEG)).build();
    LocalDate valDate = trade.getProduct().getEndDate().minusDays(7);
    ConstantNodalCurve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    DiscountFactors df = SimpleDiscountFactors.of(GBP, valDate, curve);
    TestMarketDataMap md = new TestMarketDataMap(
        valDate,
        ImmutableMap.of(DiscountCurveKey.of(GBP), curve, DiscountFactorsKey.of(GBP), df),
        ImmutableMap.of());

    assertNotNull(new SwapBucketedGammaPv01Function().execute(trade, md));
    assertNotNull(new SwapBucketedPv01Function().execute(trade, md));
    assertNotNull(new SwapExplainPvFunction().execute(trade, md));
    assertNotNull(new SwapLegPvFunction().execute(trade, md));
    assertNotNull(new SwapParRateFunction().execute(trade, md));
    assertNotNull(new SwapPv01Function().execute(trade, md));
    assertNotNull(new SwapPvFunction().execute(trade, md));
  }

}
