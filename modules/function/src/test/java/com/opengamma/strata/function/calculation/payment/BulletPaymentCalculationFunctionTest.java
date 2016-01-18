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

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Test {@link BulletPaymentCalculationFunction}.
 */
@Test
public class BulletPaymentCalculationFunctionTest {

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
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getDate().adjusted().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<BulletPaymentTrade> test = BulletPaymentFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01);
    FunctionConfig<BulletPaymentTrade> config =
        BulletPaymentFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(BulletPaymentCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    BulletPaymentCalculationFunction function = new BulletPaymentCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(DiscountCurveKey.of(CURRENCY)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.defaultReportingCurrency(TRADE)).hasValue(CURRENCY);
  }

  public void test_simpleMeasures() {
    BulletPaymentCalculationFunction function = new BulletPaymentCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(TRADE.getProduct().expandToPayment(), provider);

    Set<Measure> measures = ImmutableSet.of(Measure.PRESENT_VALUE);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measure.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))));
  }

  public void test_pv01() {
    BulletPaymentCalculationFunction function = new BulletPaymentCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(TRADE.getProduct().expandToPayment(), provider).build();
    CurveCurrencyParameterSensitivities pvParamSens = provider.curveParameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurveCurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measure.PV01, Measure.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measure.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measure.BUCKETED_PV01, Result.success(FxConvertibleList.of(ImmutableList.of(expectedBucketedPv01))));
  }

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DiscountCurveKey.of(CURRENCY), curve),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(BulletPaymentFunctionGroups.class);
    coverPrivateConstructor(BulletPaymentMeasureCalculations.class);
  }

}
