/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.dsf;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.dsf.DiscountingDsfTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.dsf.Dsf;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.dsf.ResolvedDsfTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.IborRateSwapLegConvention;

/**
 * Test {@link DsfTradeCalculationFunction}.
 */
@Test
public class DsfTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
  private static final SwapLeg FIXED_LEG =
      FixedRateSwapLegConvention.of(Currency.GBP, DayCounts.ACT_360, Frequency.P6M, BDA_MF)
          .toLeg(LocalDate.of(2013, 6, 30), LocalDate.of(2016, 6, 30), PayReceive.RECEIVE, 1, 0.001);
  private static final SwapLeg IBOR_LEG =
      IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_6M)
          .toLeg(LocalDate.of(2013, 6, 30), LocalDate.of(2016, 6, 30), PayReceive.PAY, 1);
  private static final Swap SWAP = Swap.of(FIXED_LEG, IBOR_LEG);
  private static final LocalDate LAST_TRADE = LocalDate.of(2013, 6, 17);
  private static final LocalDate DELIVERY = LocalDate.of(2013, 6, 19);
  private static final double NOTIONAL = 100000;
  private static final StandardId DSF_ID = StandardId.of("OG-Ticker", "DSF1");
  private static final Dsf FUTURE = Dsf.builder()
      .securityId(SecurityId.of(DSF_ID))
      .deliveryDate(DELIVERY)
      .lastTradeDate(LAST_TRADE)
      .notional(NOTIONAL)
      .underlyingSwap(SWAP)
      .build();
  private static final double TRADE_PRICE = 0.98 + 31.0 / 32.0 / 100.0; // price quoted in 32nd of 1%
  public static final double REF_PRICE = 0.98 + 30.0 / 32.0 / 100.0; // price quoted in 32nd of 1%
  private static final long QUANTITY = 1234L;
  public static final DsfTrade TRADE = DsfTrade.builder()
      .product(FUTURE)
      .quantity(QUANTITY)
      .price(TRADE_PRICE)
      .build();
  public static final ResolvedDsfTrade RTRADE = TRADE.resolve(REF_DATA);
  private static final Currency CURRENCY = SWAP.getPayLeg().get().getCurrency();
  public static final IborIndex INDEX = (IborIndex) SWAP.allIndices().iterator().next();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  public static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP);
  private static final LocalDate VAL_DATE = LAST_TRADE.minusDays(7);
  private static final QuoteId QUOTE_KEY = QuoteId.of(DSF_ID, FieldName.SETTLEMENT_PRICE);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    DsfTradeCalculationFunction function = new DsfTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(QUOTE_KEY, DISCOUNT_CURVE_ID, FORWARD_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    DsfTradeCalculationFunction function = new DsfTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingDsfTradePricer pricer = DiscountingDsfTradePricer.DEFAULT;
    double expectedPrice = pricer.price(RTRADE, provider);
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, REF_PRICE);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, REF_PRICE);

    Set<Measure> measures = ImmutableSet.of(
        Measures.UNIT_PRICE,
        Measures.PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.UNIT_PRICE, Result.success(DoubleScenarioArray.of(ImmutableList.of(expectedPrice))))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(ScenarioArray.ofSingleValue(1, TRADE.resolve(REF_DATA))));
  }

  public void test_pv01() {
    DsfTradeCalculationFunction function = new DsfTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingDsfTradePricer pricer = DiscountingDsfTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
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
        ImmutableMap.of(
            DISCOUNT_CURVE_ID, curve,
            FORWARD_CURVE_ID, curve,
            QUOTE_KEY, REF_PRICE),
        ImmutableMap.of());
    return md;
  }

}
