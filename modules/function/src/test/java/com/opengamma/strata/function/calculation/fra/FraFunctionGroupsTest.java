/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IborIndexRatesKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.pricer.fra.FraDummyData;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Test {@link FraFunctionGroups}.
 */
@Test
public class FraFunctionGroupsTest {

  public static final FraTrade TRADE = FraDummyData.FRA_TRADE;

  public void test_discounting() {
    FunctionGroup<FraTrade> test = FraFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PAR_RATE,
        Measure.PAR_SPREAD,
        Measure.PRESENT_VALUE,
        Measure.EXPLAIN_PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01,
        Measure.BUCKETED_GAMMA_PV01);
  }

  public void test_presentValue() {
    Currency ccy = TRADE.getProduct().getCurrency();
    IborIndex index = TRADE.getProduct().getIndex();
    LocalDate valDate = TRADE.getProduct().getEndDate().plusDays(7);

    FunctionConfig<FraTrade> config = FraFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    CalculationSingleFunction<FraTrade, ?> function = config.createFunction();
    FunctionRequirements reqs = function.requirements(TRADE);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(IborIndexRatesKey.of(index), DiscountFactorsKey.of(ccy)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(index)));
    CollectProjectAssertions.assertThat(function.defaultReportingCurrency(TRADE)).hasValue(ccy);
    DiscountFactors df = SimpleDiscountFactors.of(
        ccy, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    TestMarketDataMap md = new TestMarketDataMap(valDate, ImmutableMap.of(DiscountFactorsKey.of(ccy), df), ImmutableMap.of());
    assertThat(function.execute(TRADE, md)).isEqualTo(FxConvertibleList.of(ImmutableList.of(CurrencyAmount.of(ccy, 0d))));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FraFunctionGroups.class);
  }

}
