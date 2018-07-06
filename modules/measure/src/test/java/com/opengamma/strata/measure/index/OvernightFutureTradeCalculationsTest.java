/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.index.DiscountingOvernightFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.OvernightFuture;
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.index.ResolvedOvernightFutureTrade;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Test {@link OvernightFutureTradeCalculations}.
 */
@Test
public class OvernightFutureTradeCalculationsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
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
  private static final double ONE_BP = 1.0e-4;
  private static final double ONE_PC = 1.0e-2;
  private static final OvernightIndex INDEX = TRADE.getProduct().getIndex();
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(), ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final double SETTLEMENT_PRICE = OvernightFutureTradeCalculationFunctionTest.MARKET_PRICE * ONE_PC;
  private static final ScenarioMarketData MARKET_DATA = OvernightFutureTradeCalculationFunctionTest.marketData(
      FORWARD_CURVE_ID.getCurveName());
  private static final RatesProvider RATES_PROVIDER = RATES_LOOKUP.marketDataView(MARKET_DATA.scenario(0)).ratesProvider();
  private static final DiscountingOvernightFutureTradePricer TRADE_PRICER = DiscountingOvernightFutureTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQ_CALC = MarketQuoteSensitivityCalculator.DEFAULT;
  private static final OvernightFutureTradeCalculations CALC = OvernightFutureTradeCalculations.DEFAULT;
  private static final double TOL = 1.0e-14;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount expected = TRADE_PRICER.presentValue(RESOLVED_TRADE, RATES_PROVIDER, SETTLEMENT_PRICE);
    assertEquals(
        CALC.presentValue(RESOLVED_TRADE, RATES_LOOKUP, MARKET_DATA),
        CurrencyScenarioArray.of(ImmutableList.of(expected)));
    assertEquals(CALC.presentValue(RESOLVED_TRADE, RATES_PROVIDER), expected);
  }

  public void test_parSpread() {
    double expected = TRADE_PRICER.parSpread(RESOLVED_TRADE, RATES_PROVIDER, SETTLEMENT_PRICE);
    assertEquals(CALC.parSpread(RESOLVED_TRADE, RATES_LOOKUP, MARKET_DATA).get(0).doubleValue(), expected, TOL);
    assertEquals(CALC.parSpread(RESOLVED_TRADE, RATES_PROVIDER), expected, TOL);
  }

  public void test_unitPrice() {
    double expected = TRADE_PRICER.price(RESOLVED_TRADE, RATES_PROVIDER);
    assertEquals(CALC.unitPrice(RESOLVED_TRADE, RATES_LOOKUP, MARKET_DATA).get(0).doubleValue(), expected, TOL);
    assertEquals(CALC.unitPrice(RESOLVED_TRADE, RATES_PROVIDER), expected, TOL);
  }

  public void test_pv01_calibrated() {
    PointSensitivities pvPointSens = TRADE_PRICER.presentValueSensitivity(RESOLVED_TRADE, RATES_PROVIDER);
    CurrencyParameterSensitivities pvParamSens = RATES_PROVIDER.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(ONE_BP);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(ONE_BP);
    assertEquals(
        CALC.pv01CalibratedSum(RESOLVED_TRADE, RATES_LOOKUP, MARKET_DATA),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(CALC.pv01CalibratedSum(RESOLVED_TRADE, RATES_PROVIDER), expectedPv01Cal);
    assertEquals(
        CALC.pv01CalibratedBucketed(RESOLVED_TRADE, RATES_LOOKUP, MARKET_DATA),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
    assertEquals(CALC.pv01CalibratedBucketed(RESOLVED_TRADE, RATES_PROVIDER), expectedPv01CalBucketed);
  }

  public void test_pv01_quote() {
    PointSensitivities pvPointSens = TRADE_PRICER.presentValueSensitivity(RESOLVED_TRADE, RATES_PROVIDER);
    CurrencyParameterSensitivities pvParamSens = RATES_PROVIDER.parameterSensitivity(pvPointSens);
    CurrencyParameterSensitivities expectedPv01Bucketed = MQ_CALC.sensitivity(pvParamSens, RATES_PROVIDER).multipliedBy(ONE_BP);
    MultiCurrencyAmount expectedPv01Sum = expectedPv01Bucketed.total();
    assertEquals(
        CALC.pv01MarketQuoteSum(RESOLVED_TRADE, RATES_LOOKUP, MARKET_DATA),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Sum)));
    assertEquals(CALC.pv01MarketQuoteSum(RESOLVED_TRADE, RATES_PROVIDER), expectedPv01Sum);
    assertEquals(
        CALC.pv01MarketQuoteBucketed(RESOLVED_TRADE, RATES_LOOKUP, MARKET_DATA),
        ScenarioArray.of(ImmutableList.of(expectedPv01Bucketed)));
    assertEquals(CALC.pv01MarketQuoteBucketed(RESOLVED_TRADE, RATES_PROVIDER), expectedPv01Bucketed);
  }

}
