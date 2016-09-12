/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.payment;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Test {@link BulletPaymentTradeCalculationFunction}.
 */
@Test
public class BulletPaymentTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final BulletPayment PRODUCT = BulletPayment.builder()
      .payReceive(PayReceive.PAY)
      .value(GBP_P1000)
      .date(AdjustableDate.of(date(2015, 6, 30)))
      .build();
  public static final BulletPaymentTrade TRADE = BulletPaymentTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(PRODUCT)
      .build();
  public static final ResolvedBulletPaymentTrade RTRADE = TRADE.resolve(REF_DATA);

  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of());
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP);
  private static final LocalDate VAL_DATE = TRADE.getProduct().getDate().getUnadjusted().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    BulletPaymentTradeCalculationFunction function = new BulletPaymentTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(ImmutableSet.of(DISCOUNT_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    BulletPaymentTradeCalculationFunction function = new BulletPaymentTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    Payment payment = RTRADE.getProduct().getPayment();
    CurrencyAmount expectedPv = pricer.presentValue(payment, provider);
    CashFlows expectedCashFlows = pricer.cashFlows(payment, provider);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.CASH_FLOWS,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CASH_FLOWS, Result.success(ScenarioArray.of(ImmutableList.of(expectedCashFlows))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RTRADE));

  }

  public void test_pv01() {
    BulletPaymentTradeCalculationFunction function = new BulletPaymentTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    Payment payment = RTRADE.getProduct().getPayment();
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(payment, provider).build();
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_CALIBRATED_SUM,
        Measures.PV01_CALIBRATED_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01_CALIBRATED_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.PV01_CALIBRATED_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedBucketedPv01))));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DISCOUNT_CURVE_ID, curve),
        ImmutableMap.of());
    return md;
  }

}
