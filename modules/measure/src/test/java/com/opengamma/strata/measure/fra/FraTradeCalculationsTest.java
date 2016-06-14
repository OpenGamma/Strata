/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fra;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.fra.FraDummyData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFraTrade;

/**
 * Test {@link FraTradeCalculations}.
 */
@Test
public class FraTradeCalculationsTest {

  public static final FraTrade TRADE = FraDummyData.FRA_TRADE;

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex INDEX = TRADE.getProduct().getIndex();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final RatesMarketDataLookup RATES_MODEL = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final LocalDate VAL_DATE = TRADE.getProduct().getStartDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_MODEL.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFraTradePricer pricer = DiscountingFraTradePricer.DEFAULT;
    ResolvedFraTrade resolved = TRADE.resolve(REF_DATA);
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider);
    double expectedParRate = pricer.parRate(resolved, provider);
    double expectedParSpread = pricer.parSpread(resolved, provider);
    CashFlows expectedCashFlows = pricer.cashFlows(resolved, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(resolved, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(resolved, provider);
    ExplainMap expectedExplainPv = pricer.explainPresentValue(resolved, provider);

    assertEquals(
        FraTradeCalculations.DEFAULT.presentValue(resolved, RATES_MODEL, md),
        CurrencyValuesArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        FraTradeCalculations.DEFAULT.parRate(resolved, RATES_MODEL, md),
        ValuesArray.of(ImmutableList.of(expectedParRate)));
    assertEquals(
        FraTradeCalculations.DEFAULT.parSpread(resolved, RATES_MODEL, md),
        ValuesArray.of(ImmutableList.of(expectedParSpread)));
    assertEquals(
        FraTradeCalculations.DEFAULT.cashFlows(resolved, RATES_MODEL, md),
        ScenarioArray.of(ImmutableList.of(expectedCashFlows)));
    assertEquals(
        FraTradeCalculations.DEFAULT.currencyExposure(resolved, RATES_MODEL, md),
        MultiCurrencyValuesArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        FraTradeCalculations.DEFAULT.currentCash(resolved, RATES_MODEL, md),
        CurrencyValuesArray.of(ImmutableList.of(expectedCurrentCash)));
    assertEquals(
        FraTradeCalculations.DEFAULT.explainPresentValue(resolved, RATES_MODEL, md),
        ScenarioArray.of(ImmutableList.of(expectedExplainPv)));
  }

  public void test_pv01() {
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_MODEL.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFraTradePricer pricer = DiscountingFraTradePricer.DEFAULT;
    ResolvedFraTrade resolved = TRADE.resolve(REF_DATA);
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(resolved, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        FraTradeCalculations.DEFAULT.pv01CalibratedSum(resolved, RATES_MODEL, md),
        MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        FraTradeCalculations.DEFAULT.pv01CalibratedBucketed(resolved, RATES_MODEL, md),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
  }

  //-------------------------------------------------------------------------
  private ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    return new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DISCOUNT_CURVE_ID, curve, FORWARD_CURVE_ID, curve),
        ImmutableMap.of());
  }

}
