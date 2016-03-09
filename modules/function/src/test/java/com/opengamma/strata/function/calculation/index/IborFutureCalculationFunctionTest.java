/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureConventions;

/**
 * Test {@link IborFutureCalculationFunction}.
 */
@Test
public class IborFutureCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double MARKET_PRICE = 99.42;
  public static final IborFutureTrade TRADE = IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM.createTrade(
      LocalDate.of(2014, 9, 12), Period.ofMonths(1), 2, 5, 1_000_000, 0.9998, REF_DATA);

  private static final StandardId SEC_ID = TRADE.getProduct().getSecurityId().getStandardId();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final IborIndex INDEX = TRADE.getProduct().getIndex();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getLastTradeDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<IborFutureTrade> test = IborFutureFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PRESENT_VALUE);
    FunctionConfig<IborFutureTrade> config =
        IborFutureFunctionGroups.discounting().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(IborFutureCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    IborFutureCalculationFunction function = new IborFutureCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountCurveKey.of(CURRENCY), IborIndexCurveKey.of(INDEX), QuoteKey.of(SEC_ID)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    IborFutureCalculationFunction function = new IborFutureCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    ResolvedIborFutureTrade resolved = TRADE.resolve(REF_DATA);
    CurrencyAmount expectedPv = DiscountingIborFutureTradePricer.DEFAULT.presentValue(resolved, provider, MARKET_PRICE / 100);
    double expectedParSpread = DiscountingIborFutureTradePricer.DEFAULT.parSpread(resolved, provider, MARKET_PRICE / 100);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.PRESENT_VALUE_MULTI_CCY,
        Measures.PAR_SPREAD);
    assertThat(function.calculate(TRADE, measures, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))));
  }

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DiscountCurveKey.of(CURRENCY), curve,
            IborIndexCurveKey.of(INDEX), curve,
            QuoteKey.of(SEC_ID), MARKET_PRICE),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(IborFutureFunctionGroups.class);
    coverPrivateConstructor(IborFutureMeasureCalculations.class);
  }

}
