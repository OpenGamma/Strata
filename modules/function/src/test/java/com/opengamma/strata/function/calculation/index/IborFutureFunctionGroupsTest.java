/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.collect.CollectProjectAssertions;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IborIndexRatesKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.DiscountIborIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureConventions;

/**
 * Test {@link IborFutureFunctionGroups}.
 */
@Test
public class IborFutureFunctionGroupsTest {

  public static final IborFutureTrade TRADE = IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM.toTrade(
      LocalDate.of(2014, 9, 12), Period.ofMonths(1), 2, 5, 1_000_000, 0.9998);

  public void test_discounting() {
    FunctionGroup<IborFutureTrade> test = IborFutureFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PAR_SPREAD,
        Measure.PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01);
  }

  public void test_presentValue() {
    Currency ccy = TRADE.getProduct().getCurrency();
    IborIndex index = TRADE.getProduct().getIndex();
    LocalDate valDate = TRADE.getProduct().getLastTradeDate().plusDays(7);

    FunctionConfig<IborFutureTrade> config =
        IborFutureFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    CalculationSingleFunction<IborFutureTrade, ?> function = config.createFunction();
    FunctionRequirements reqs = function.requirements(TRADE);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy);

    QuoteKey quoteKey = QuoteKey.of(TRADE.getSecurity().getStandardId());
    DiscountFactorsKey dfKey = DiscountFactorsKey.of(ccy);
    IborIndexRatesKey iborKey = IborIndexRatesKey.of(index);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(iborKey, dfKey, quoteKey));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(index)));
    CollectProjectAssertions.assertThat(function.defaultReportingCurrency(TRADE)).hasValue(ccy);
    DiscountFactors df = SimpleDiscountFactors.of(
        ccy, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(date(2015, 3, 16), 0.0015);
    IborIndexRates iir = DiscountIborIndexRates.of(index, ts, df);
    TestMarketDataMap md = new TestMarketDataMap(
        valDate,
        ImmutableMap.of(dfKey, df, iborKey, iir, quoteKey, 99.995),
        ImmutableMap.of());
    assertThat(function.execute(TRADE, md)).isEqualTo(
        FxConvertibleList.of(ImmutableList.of(CurrencyAmount.of(ccy, -1812.5d))));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(IborFutureFunctionGroups.class);
  }

}
