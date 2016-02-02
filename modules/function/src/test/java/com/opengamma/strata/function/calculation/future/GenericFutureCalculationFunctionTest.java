/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.future;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.future.GenericFuture;
import com.opengamma.strata.product.future.GenericFutureTrade;

/**
 * Test {@link GenericFutureCalculationFunction}.
 */
@Test
public class GenericFutureCalculationFunctionTest {

  private static final double MARKET_PRICE = 99.42;
  private static final double TICK_SIZE = 0.01;
  private static final int TICK_VALUE = 10;
  private static final int QUANTITY = 20;
  private static final StandardId SEC_ID = StandardId.of("OG-Future", "Foo-Womble-Mar14");
  private static final GenericFuture FUTURE = GenericFuture.builder()
      .productId(StandardId.of("Foo", "Womble"))
      .expiryMonth(YearMonth.of(2014, 3))
      .expiryDate(LocalDate.of(2014, 3, 13))
      .tickSize(TICK_SIZE)
      .tickValue(CurrencyAmount.of(EUR, TICK_VALUE))
      .build();
  public static final GenericFutureTrade TRADE = GenericFutureTrade.builder()
      .securityLink(SecurityLink.resolved(
          UnitSecurity.builder(FUTURE)
              .standardId(SEC_ID)
              .build()))
      .tradeInfo(TradeInfo.builder()
          .settlementDate(LocalDate.of(2013, 12, 15))
          .build())
      .quantity(QUANTITY)
      .initialPrice(99.550)
      .build();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getExpiryDate().get().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<GenericFutureTrade> test = GenericFutureFunctionGroups.market();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PRESENT_VALUE);
    FunctionConfig<GenericFutureTrade> config =
        GenericFutureFunctionGroups.market().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(GenericFutureCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    GenericFutureCalculationFunction function = new GenericFutureCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(QuoteKey.of(SEC_ID)));
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    assertThat(function.naturalCurrency(TRADE)).isEqualTo(CURRENCY);
  }

  public void test_presentValue() {
    GenericFutureCalculationFunction function = new GenericFutureCalculationFunction();
    CalculationMarketData md = marketData();
    
    double unitPv = (MARKET_PRICE / TICK_SIZE) * TICK_VALUE;
    CurrencyAmount expectedPv = CurrencyAmount.of(CURRENCY, unitPv * QUANTITY);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))));
  }

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(QuoteKey.of(SEC_ID), MARKET_PRICE),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(GenericFutureFunctionGroups.class);
    coverPrivateConstructor(GenericFutureMeasureCalculations.class);
  }

}
