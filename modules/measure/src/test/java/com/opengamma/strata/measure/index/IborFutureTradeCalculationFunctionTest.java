/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureConventions;

/**
 * Test {@link IborFutureTradeCalculationFunction}.
 */
@Test
public class IborFutureTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double MARKET_PRICE = 99.42;
  public static final IborFutureTrade TRADE = IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM.createTrade(
      LocalDate.of(2014, 9, 12), SecurityId.of("test", "test"), Period.ofMonths(1), 2, 5, 1_000_000, 0.9998, REF_DATA);
  public static final ResolvedIborFutureTrade RTRADE = TRADE.resolve(REF_DATA);

  private static final StandardId SEC_ID = TRADE.getProduct().getSecurityId().getStandardId();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final IborIndex INDEX = TRADE.getProduct().getIndex();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP);
  private static final LocalDate VAL_DATE = TRADE.getProduct().getLastTradeDate().minusDays(7);
  private static final QuoteId QUOTE_KEY = QuoteId.of(SEC_ID, FieldName.SETTLEMENT_PRICE);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    IborFutureTradeCalculationFunction function = new IborFutureTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_ID, FORWARD_CURVE_ID, QUOTE_KEY));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    IborFutureTradeCalculationFunction function = new IborFutureTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    double expectedPrice = DiscountingIborFutureTradePricer.DEFAULT.price(RTRADE, provider);
    CurrencyAmount expectedPv = DiscountingIborFutureTradePricer.DEFAULT.presentValue(RTRADE, provider, MARKET_PRICE / 100);
    double expectedParSpread = DiscountingIborFutureTradePricer.DEFAULT.parSpread(RTRADE, provider, MARKET_PRICE / 100);

    Set<Measure> measures = ImmutableSet.of(
        Measures.UNIT_PRICE,
        Measures.PRESENT_VALUE,
        Measures.PAR_SPREAD,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.UNIT_PRICE, Result.success(DoubleScenarioArray.of(ImmutableList.of(expectedPrice))))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(DoubleScenarioArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(ScenarioArray.ofSingleValue(1, TRADE.resolve(REF_DATA))));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DISCOUNT_CURVE_ID, curve,
            FORWARD_CURVE_ID, curve,
            QUOTE_KEY, MARKET_PRICE),
        ImmutableMap.of());
    return md;
  }

}
