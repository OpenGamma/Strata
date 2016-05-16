/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.id.SimpleCurveId;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Test {@link RatesMarketDataLookup}.
 */
@Test
public class RatesMarketDataLookupTest {

  private static final SimpleCurveId CURVE_ID_DSC = SimpleCurveId.of("Group", "USD-DSC");
  private static final SimpleCurveId CURVE_ID_FWD = SimpleCurveId.of("Group", "USD-L3M");
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final CalculationMarketData MOCK_CALC_MARKET_DATA = mock(CalculationMarketData.class);

  //-------------------------------------------------------------------------
  public void test_of_map() {
    ImmutableMap<Currency, SimpleCurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, SimpleCurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    assertEquals(test.queryType(), RatesMarketDataLookup.class);
    assertEquals(test.getDiscountCurrencies(), ImmutableSet.of(USD));
    assertEquals(test.getDiscountMarketDataIds(USD), ImmutableSet.of(CURVE_ID_DSC));
    assertEquals(test.getForwardIndices(), ImmutableSet.of(USD_LIBOR_3M));
    assertEquals(test.getForwardMarketDataIds(USD_LIBOR_3M), ImmutableSet.of(CURVE_ID_FWD));
    assertThrowsIllegalArg(() -> test.getDiscountMarketDataIds(GBP));
    assertThrowsIllegalArg(() -> test.getForwardMarketDataIds(GBP_LIBOR_3M));

    assertEquals(
        test.requirements(USD),
        FunctionRequirements.builder().singleValueRequirements(CURVE_ID_DSC).outputCurrencies(USD).build());
    assertEquals(
        test.requirements(USD, USD_LIBOR_3M),
        FunctionRequirements.builder()
            .singleValueRequirements(CURVE_ID_DSC, CURVE_ID_FWD)
            .timeSeriesRequirements(IndexRateKey.of(USD_LIBOR_3M))
            .outputCurrencies(USD)
            .build());
    assertEquals(
        test.requirements(ImmutableSet.of(USD), ImmutableSet.of(USD_LIBOR_3M)),
        FunctionRequirements.builder()
            .singleValueRequirements(CURVE_ID_DSC, CURVE_ID_FWD)
            .timeSeriesRequirements(IndexRateKey.of(USD_LIBOR_3M))
            .outputCurrencies(USD)
            .build());
    assertThrowsIllegalArg(() -> test.requirements(ImmutableSet.of(USD), ImmutableSet.of(GBP_LIBOR_3M)));

    assertEquals(
        test.ratesProvider(MOCK_MARKET_DATA),
        DefaultLookupRatesProvider.of((DefaultRatesMarketDataLookup) test, MOCK_MARKET_DATA));
  }

  public void test_of_groupNameAndMap() {
    ImmutableMap<Currency, CurveName> discounts = ImmutableMap.of(USD, CURVE_ID_DSC.getCurveName());
    ImmutableMap<Index, CurveName> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD.getCurveName());
    RatesMarketDataLookup test = RatesMarketDataLookup.of(CURVE_ID_DSC.getCurveGroupName(), discounts, forwards);
    assertEquals(test.queryType(), RatesMarketDataLookup.class);
    assertEquals(test.getDiscountCurrencies(), ImmutableSet.of(USD));
    assertEquals(test.getDiscountMarketDataIds(USD), ImmutableSet.of(CURVE_ID_DSC));
    assertEquals(test.getForwardIndices(), ImmutableSet.of(USD_LIBOR_3M));
    assertEquals(test.getForwardMarketDataIds(USD_LIBOR_3M), ImmutableSet.of(CURVE_ID_FWD));
    assertThrowsIllegalArg(() -> test.getDiscountMarketDataIds(GBP));
    assertThrowsIllegalArg(() -> test.getForwardMarketDataIds(GBP_LIBOR_3M));
  }

  public void test_of_curveGroup() {
    ImmutableMap<Currency, Curve> discounts = ImmutableMap.of(USD, ConstantNodalCurve.of(CURVE_ID_DSC.getCurveName(), 1));
    ImmutableMap<Index, Curve> forwards = ImmutableMap.of(USD_LIBOR_3M, ConstantNodalCurve.of(CURVE_ID_FWD.getCurveName(), 1));
    CurveGroup group = CurveGroup.of(CURVE_ID_DSC.getCurveGroupName(), discounts, forwards);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(group);
    assertEquals(test.queryType(), RatesMarketDataLookup.class);
    assertEquals(test.getDiscountCurrencies(), ImmutableSet.of(USD));
    assertEquals(test.getDiscountMarketDataIds(USD), ImmutableSet.of(CURVE_ID_DSC));
    assertEquals(test.getForwardIndices(), ImmutableSet.of(USD_LIBOR_3M));
    assertEquals(test.getForwardMarketDataIds(USD_LIBOR_3M), ImmutableSet.of(CURVE_ID_FWD));
    assertThrowsIllegalArg(() -> test.getDiscountMarketDataIds(GBP));
    assertThrowsIllegalArg(() -> test.getForwardMarketDataIds(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void test_marketDataView() {
    ImmutableMap<Currency, SimpleCurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, SimpleCurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    LocalDate valDate = date(2015, 6, 30);
    CalculationMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    RatesScenarioMarketData multiScenario = test.marketDataView(md);
    assertEquals(multiScenario.getLookup(), test);
    assertEquals(multiScenario.getMarketData(), md);
    assertEquals(multiScenario.getScenarioCount(), 1);
    RatesMarketData scenario = multiScenario.scenario(0);
    assertEquals(scenario.getLookup(), test);
    assertEquals(scenario.getMarketData(), md.scenario(0));
    assertEquals(scenario.getValuationDate(), valDate);
  }

  public void test_ratesProvider() {
    ImmutableMap<Currency, SimpleCurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, SimpleCurveId> forwards =
        ImmutableMap.of(USD_FED_FUND, CURVE_ID_DSC, USD_LIBOR_3M, CURVE_ID_FWD, US_CPI_U, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    LocalDate valDate = date(2015, 6, 30);
    Curve dscCurve = ConstantNodalCurve.of(Curves.discountFactors(CURVE_ID_DSC.getCurveName(), ACT_360), 1d);
    Curve fwdCurve = ConstantNodalCurve.of(Curves.discountFactors(CURVE_ID_FWD.getCurveName(), ACT_360), 2d);
    MarketData md = ImmutableMarketData.of(valDate, ImmutableMap.of(CURVE_ID_DSC, dscCurve, CURVE_ID_FWD, fwdCurve));
    RatesProvider ratesProvider = test.ratesProvider(md);
    assertEquals(ratesProvider.getValuationDate(), valDate);
    assertEquals(ratesProvider.findCurve(CURVE_ID_DSC.getCurveName()), Optional.of(dscCurve));
    assertEquals(ratesProvider.findCurve(CURVE_ID_FWD.getCurveName()), Optional.of(fwdCurve));
    assertEquals(ratesProvider.findCurve(CurveName.of("Rubbish")), Optional.empty());
    // check curve lookup
    assertEquals(ratesProvider.discountFactors(USD).getCurveName(), dscCurve.getName());
    assertThrowsIllegalArg(() -> ratesProvider.discountFactors(GBP));
    assertEquals(ratesProvider.iborIndexRates(USD_LIBOR_3M).getCurveName(), fwdCurve.getName());
    assertThrowsIllegalArg(() -> ratesProvider.iborIndexRates(GBP_LIBOR_3M));
    assertEquals(ratesProvider.overnightIndexRates(USD_FED_FUND).getCurveName(), dscCurve.getName());
    assertThrowsIllegalArg(() -> ratesProvider.overnightIndexRates(GBP_SONIA));
    // price curve must be interpolated
    assertThrowsIllegalArg(() -> ratesProvider.priceIndexValues(US_CPI_U).getCurveName());
    assertThrowsIllegalArg(() -> ratesProvider.priceIndexValues(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableMap<Currency, SimpleCurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, SimpleCurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test = DefaultRatesMarketDataLookup.of(discounts, forwards);
    coverImmutableBean(test);

    ImmutableMap<Currency, SimpleCurveId> discounts2 = ImmutableMap.of(GBP, CURVE_ID_DSC);
    ImmutableMap<Index, SimpleCurveId> forwards2 = ImmutableMap.of(GBP_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test2 = DefaultRatesMarketDataLookup.of(discounts2, forwards2);
    coverBeanEquals(test, test2);

    // related coverage
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    DefaultRatesScenarioMarketData.meta();

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
    DefaultRatesMarketData.meta();

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA).ratesProvider());
    DefaultLookupRatesProvider.meta();
  }

  public void test_serialization() {
    ImmutableMap<Currency, SimpleCurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, SimpleCurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test = DefaultRatesMarketDataLookup.of(discounts, forwards);
    assertSerialization(test);
  }

}
