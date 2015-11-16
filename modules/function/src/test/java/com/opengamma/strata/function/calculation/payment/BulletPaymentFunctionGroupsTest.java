/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.payment;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
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
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Test {@link BulletPaymentFunctionGroups}.
 */
@Test
public class BulletPaymentFunctionGroupsTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final BulletPayment PRODUCT = BulletPayment.builder()
      .payReceive(PayReceive.PAY)
      .value(GBP_P1000)
      .date(AdjustableDate.of(date(2015, 6, 30)))
      .build();
  public static final BulletPaymentTrade TRADE = BulletPaymentTrade.builder()
      .tradeInfo(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(PRODUCT)
      .build();

  public void test_discounting() {
    FunctionGroup<BulletPaymentTrade> test = BulletPaymentFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01);
  }

  public void test_presentValue() {
    Currency ccy = TRADE.getProduct().getCurrency();
    LocalDate valDate = TRADE.getProduct().getDate().adjusted().plusDays(7);

    FunctionConfig<BulletPaymentTrade> config =
        BulletPaymentFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    CalculationSingleFunction<BulletPaymentTrade, ?> function = config.createFunction();
    FunctionRequirements reqs = function.requirements(TRADE);
    assertThat(reqs.getOutputCurrencies()).containsOnly(ccy);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(DiscountFactorsKey.of(ccy)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    CollectProjectAssertions.assertThat(function.defaultReportingCurrency(TRADE)).hasValue(ccy);
    DiscountFactors df1 = SimpleDiscountFactors.of(
        ccy, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    TestMarketDataMap md = new TestMarketDataMap(
        valDate,
        ImmutableMap.of(DiscountFactorsKey.of(ccy), df1),
        ImmutableMap.of());
    assertThat(function.execute(TRADE, md)).isEqualTo(FxConvertibleList.of(ImmutableList.of(CurrencyAmount.zero(ccy))));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(BulletPaymentFunctionGroups.class);
  }

  public void coverage_functions() {
    Currency ccy = TRADE.getProduct().getCurrency();
    LocalDate valDate = TRADE.getProduct().getDate().adjusted().plusDays(7);
    DiscountFactors df1 = SimpleDiscountFactors.of(
        ccy, valDate, ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99));
    TestMarketDataMap md = new TestMarketDataMap(
        valDate,
        ImmutableMap.of(DiscountFactorsKey.of(ccy), df1),
        ImmutableMap.of());

    assertNotNull(new BulletPaymentBucketedPv01Function().execute(TRADE, md));
    assertNotNull(new BulletPaymentPv01Function().execute(TRADE, md));
    assertNotNull(new BulletPaymentPvFunction().execute(TRADE, md));
  }

}
