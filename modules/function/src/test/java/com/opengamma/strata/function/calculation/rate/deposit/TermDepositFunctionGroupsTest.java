/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.deposit;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_360_ISDA;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.CollectProjectAssertions;
import com.opengamma.strata.engine.calculation.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculation.function.result.DefaultScenarioResult;
import com.opengamma.strata.engine.calculation.function.result.FxConvertibleList;
import com.opengamma.strata.engine.config.FunctionConfig;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.deposit.TermDeposit;
import com.opengamma.strata.finance.rate.deposit.TermDepositTrade;
import com.opengamma.strata.function.marketdata.curve.MarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.SimpleDiscountFactors;

/**
 * Test {@link TermDepositFunctionGroups}.
 */
@Test
public class TermDepositFunctionGroupsTest {

  private static final TermDepositTrade TD_TRADE = TermDepositTrade.builder()
      .tradeInfo(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(TermDeposit.builder()
          .buySell(BuySell.BUY)
          .startDate(date(2015, 6, 1))
          .endDate(date(2015, 9, 1))
          .currency(Currency.GBP)
          .notional(10000000d)
          .dayCount(THIRTY_360_ISDA)
          .rate(0.002)
          .build())
      .build();

  public void test_discounting() {
    FunctionGroup<TermDepositTrade> test = TermDepositFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TD_TRADE)).contains(
        Measure.PAR_RATE,
        Measure.PAR_SPREAD,
        Measure.PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01);
  }

  public void test_presentValue() {
    Currency ccy = TD_TRADE.getProduct().getCurrency();
    LocalDate valDate = TD_TRADE.getProduct().getEndDate().plusDays(7);

    FunctionConfig<TermDepositTrade> config =
        TermDepositFunctionGroups.discounting().functionConfig(TD_TRADE, Measure.PRESENT_VALUE).get();
    CalculationSingleFunction<TermDepositTrade, ?> function = config.createFunction();
    FunctionRequirements reqs = function.requirements(TD_TRADE);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(DiscountFactorsKey.of(ccy)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    CollectProjectAssertions.assertThat(function.defaultReportingCurrency(TD_TRADE)).hasValue(ccy);
    DiscountFactors df = SimpleDiscountFactors.of(
        ccy, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    MarketDataMap md = new MarketDataMap(valDate, ImmutableMap.of(DiscountFactorsKey.of(ccy), df), ImmutableMap.of());
    assertThat(function.execute(TD_TRADE, md)).isEqualTo(FxConvertibleList.of(ImmutableList.of(CurrencyAmount.of(ccy, 0d))));
  }

  public void test_parRate() {
    Currency ccy = TD_TRADE.getProduct().getCurrency();
    LocalDate valDate = TD_TRADE.getProduct().getEndDate().plusDays(7);

    FunctionConfig<TermDepositTrade> config =
        TermDepositFunctionGroups.discounting().functionConfig(TD_TRADE, Measure.PAR_RATE).get();
    CalculationSingleFunction<TermDepositTrade, ?> function = config.createFunction();
    FunctionRequirements reqs = function.requirements(TD_TRADE);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(DiscountFactorsKey.of(ccy)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    CollectProjectAssertions.assertThat(function.defaultReportingCurrency(TD_TRADE)).hasValue(ccy);
    DiscountFactors df = SimpleDiscountFactors.of(
        ccy, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    MarketDataMap md = new MarketDataMap(valDate, ImmutableMap.of(DiscountFactorsKey.of(ccy), df), ImmutableMap.of());
    assertThat(function.execute(TD_TRADE, md)).isEqualTo(DefaultScenarioResult.of(0d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(TermDepositFunctionGroups.class);
  }

}
