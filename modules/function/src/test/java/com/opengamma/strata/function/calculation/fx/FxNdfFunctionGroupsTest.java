/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateKey;
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
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;

/**
 * Test {@link FxNdfFunctionGroups}.
 */
@Test
public class FxNdfFunctionGroupsTest {

  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final CurrencyAmount NOTIONAL = CurrencyAmount.of(GBP, (double) 100_000_000);
  private static final FxNdf PRODUCT = FxNdf.builder()
      .agreedFxRate(FX_RATE)
      .settlementCurrencyNotional(NOTIONAL)
      .index(GBP_USD_WM)
      .paymentDate(date(2015, 3, 19))
      .build();
  public static final FxNdfTrade TRADE = FxNdfTrade.builder()
      .tradeInfo(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(PRODUCT)
      .build();

  public void test_discounting() {
    FunctionGroup<FxNdfTrade> test = FxNdfFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01,
        Measure.CURRENCY_EXPOSURE,
        Measure.FORWARD_FX_RATE);
  }

  public void test_presentValue() {
    Currency ccy1 = TRADE.getProduct().getNonDeliverableCurrency();
    Currency ccy2 = TRADE.getProduct().getSettlementCurrency();
    LocalDate valDate = TRADE.getProduct().getPaymentDate().plusDays(7);

    FunctionConfig<FxNdfTrade> config =
        FxNdfFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    CalculationSingleFunction<FxNdfTrade, ?> function = config.createFunction();
    FunctionRequirements reqs = function.requirements(TRADE);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy1, ccy2);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountFactorsKey.of(ccy1), DiscountFactorsKey.of(ccy2)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    CollectProjectAssertions.assertThat(function.defaultReportingCurrency(TRADE)).hasValue(GBP);
    DiscountFactors df1 = SimpleDiscountFactors.of(
        ccy1, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    DiscountFactors df2 = SimpleDiscountFactors.of(
        ccy2, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    TestMarketDataMap md = new TestMarketDataMap(
        valDate,
        ImmutableMap.of(DiscountFactorsKey.of(ccy1), df1, DiscountFactorsKey.of(ccy2), df2),
        ImmutableMap.of());
    assertThat(function.execute(TRADE, md)).isEqualTo(FxConvertibleList.of(ImmutableList.of(CurrencyAmount.zero(GBP))));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FxNdfFunctionGroups.class);
  }

  public void coverage_functions() {
    Currency ccy1 = TRADE.getProduct().getNonDeliverableCurrency();
    Currency ccy2 = TRADE.getProduct().getSettlementCurrency();
    LocalDate valDate = TRADE.getProduct().getPaymentDate().plusDays(7);
    DiscountFactors df1 = SimpleDiscountFactors.of(
        ccy1, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    DiscountFactors df2 = SimpleDiscountFactors.of(
        ccy2, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    FxRate fxRate = FxRate.of(ccy1, ccy2, 1.6d);
    TestMarketDataMap md = new TestMarketDataMap(
        valDate,
        ImmutableMap.of(
            DiscountFactorsKey.of(ccy1), df1,
            DiscountFactorsKey.of(ccy2), df2,
            FxRateKey.of(ccy1, ccy2), fxRate),
        ImmutableMap.of(
            IndexRateKey.of(GBP_USD_WM), LocalDateDoubleTimeSeries.of(date(2015, 3, 17), 1.45d)));

    assertNotNull(new FxNdfBucketedPv01Function().execute(TRADE, md));
    assertNotNull(new FxNdfCurrencyExposureFunction().execute(TRADE, md));
    assertNotNull(new FxNdfForwardFxRateFunction().execute(TRADE, md));
    assertNotNull(new FxNdfPv01Function().execute(TRADE, md));
    assertNotNull(new FxNdfPvFunction().execute(TRADE, md));
  }

}
