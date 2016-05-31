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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Measures;
import com.opengamma.strata.calc.ScenarioMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.result.CurrencyValuesArray;
import com.opengamma.strata.calc.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.result.ScenarioResult;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.calculation.RatesMarketDataLookup;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.id.CurveId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
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
      .info(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(PRODUCT)
      .build();

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of());
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP);
  private static final LocalDate VAL_DATE = TRADE.getProduct().getDate().getUnadjusted().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    BulletPaymentCalculationFunction function = new BulletPaymentCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(DISCOUNT_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    BulletPaymentCalculationFunction function = new BulletPaymentCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    Payment resolved = TRADE.getProduct().resolve(REF_DATA).getPayment();
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))));
  }

  public void test_pv01() {
    BulletPaymentCalculationFunction function = new BulletPaymentCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingPaymentPricer pricer = DiscountingPaymentPricer.DEFAULT;
    Payment resolved = TRADE.getProduct().resolve(REF_DATA).getPayment();
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(resolved, provider).build();
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measures.PV01, Measures.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.BUCKETED_PV01, Result.success(ScenarioResult.of(ImmutableList.of(expectedBucketedPv01))));
  }

  //-------------------------------------------------------------------------
  private ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DISCOUNT_CURVE_ID, curve),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(BulletPaymentMeasureCalculations.class);
  }

}
