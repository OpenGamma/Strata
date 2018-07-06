/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE;
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
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.index.DiscountingOvernightFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.OvernightFuture;
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.index.ResolvedOvernightFutureTrade;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Test {@link OvernightFutureTradeCalculationFunction}.
 */
@Test
public class OvernightFutureTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2018, 8, 18);
  public static final double MARKET_PRICE = 99.42;
  private static final LocalDate TRADE_DATE = date(2018, 3, 18);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(TRADE_DATE);
  private static final double NOTIONAL = 5_000_000d;
  private static final double ACCRUAL_FACTOR = TENOR_1M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2018, 9, 28);
  private static final LocalDate START_DATE = date(2018, 9, 1);
  private static final LocalDate END_DATE = date(2018, 9, 30);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(5);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "OnFuture");
  private static final OvernightFuture PRODUCT = OvernightFuture.builder()
      .securityId(SECURITY_ID)
      .currency(USD)
      .notional(NOTIONAL)
      .accrualFactor(ACCRUAL_FACTOR)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .lastTradeDate(LAST_TRADE_DATE)
      .index(USD_FED_FUND)
      .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
      .rounding(ROUNDING)
      .build();
  private static final double QUANTITY = 35;
  private static final double PRICE = 0.998;
  private static final OvernightFutureTrade TRADE = OvernightFutureTrade.builder()
      .info(TRADE_INFO)
      .product(PRODUCT)
      .quantity(QUANTITY)
      .price(PRICE)
      .build();
  private static final ResolvedOvernightFutureTrade RESOLVED_TRADE = TRADE.resolve(REF_DATA);

  private static final StandardId SEC_ID = TRADE.getProduct().getSecurityId().getStandardId();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final OvernightIndex INDEX = TRADE.getProduct().getIndex();
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(), ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP);
  private static final QuoteId QUOTE_KEY = QuoteId.of(SEC_ID, FieldName.SETTLEMENT_PRICE);
  private static final DiscountingOvernightFutureTradePricer TRADE_PRICER = DiscountingOvernightFutureTradePricer.DEFAULT;

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    OvernightFutureTradeCalculationFunction<OvernightFutureTrade> function = OvernightFutureTradeCalculationFunction.TRADE;
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).isEmpty();
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(FORWARD_CURVE_ID, QUOTE_KEY));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    OvernightFutureTradeCalculationFunction<OvernightFutureTrade> function = OvernightFutureTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData(FORWARD_CURVE_ID.getCurveName());
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    double expectedPrice = TRADE_PRICER.price(RESOLVED_TRADE, provider);
    CurrencyAmount expectedPv = TRADE_PRICER.presentValue(RESOLVED_TRADE, provider, MARKET_PRICE / 100d);
    double expectedParSpread = TRADE_PRICER.parSpread(RESOLVED_TRADE, provider, MARKET_PRICE / 100d);

    Set<Measure> measures = ImmutableSet.of(
        Measures.UNIT_PRICE,
        Measures.PRESENT_VALUE,
        Measures.PAR_SPREAD,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.UNIT_PRICE, Result.success(DoubleScenarioArray.of(ImmutableList.of(expectedPrice))))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(DoubleScenarioArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RESOLVED_TRADE));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData(CurveName curveName) {
    DoubleMatrix jacobian = DoubleMatrix.ofUnsafe(new double[][] {
        {0.985d, 0.01d, 0d}, {0.01d, 0.98d, 0.01d}, {0.005d, 0.01d, 0.99d}});
    JacobianCalibrationMatrix jcm = JacobianCalibrationMatrix.of(ImmutableList.of(CurveParameterSize.of(curveName, 3)), jacobian);
    DoubleArray time = DoubleArray.of(0.1, 0.25, 0.5d);
    DoubleArray rate = DoubleArray.of(0.01, 0.015, 0.008d);
    Curve curve = InterpolatedNodalCurve.of(
        Curves.zeroRates(curveName, ACT_360).withInfo(CurveInfoType.JACOBIAN, jcm), time, rate, NATURAL_SPLINE);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(FORWARD_CURVE_ID, curve, QUOTE_KEY, MARKET_PRICE),
        ImmutableMap.of());
    return md;
  }

}
