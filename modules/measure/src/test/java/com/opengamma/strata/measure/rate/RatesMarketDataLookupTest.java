/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataFxRateProvider;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.rate.DiscountIborIndexRates;
import com.opengamma.strata.pricer.rate.DiscountOvernightIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Test {@link RatesMarketDataLookup}.
 */
@Test
public class RatesMarketDataLookupTest {

  private static final CurveId CURVE_ID_DSC = CurveId.of("Group", "USD-DSC");
  private static final CurveId CURVE_ID_FWD = CurveId.of("Group", "USD-L3M");
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);

  //-------------------------------------------------------------------------
  public void test_of_map() {
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
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
        FunctionRequirements.builder().valueRequirements(CURVE_ID_DSC).outputCurrencies(USD).build());
    assertEquals(
        test.requirements(USD, USD_LIBOR_3M),
        FunctionRequirements.builder()
            .valueRequirements(CURVE_ID_DSC, CURVE_ID_FWD)
            .timeSeriesRequirements(IndexQuoteId.of(USD_LIBOR_3M))
            .outputCurrencies(USD)
            .build());
    assertEquals(
        test.requirements(ImmutableSet.of(USD), ImmutableSet.of(USD_LIBOR_3M)),
        FunctionRequirements.builder()
            .valueRequirements(CURVE_ID_DSC, CURVE_ID_FWD)
            .timeSeriesRequirements(IndexQuoteId.of(USD_LIBOR_3M))
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
    ImmutableMap<Currency, Curve> discounts = ImmutableMap.of(USD, ConstantCurve.of(CURVE_ID_DSC.getCurveName(), 1));
    ImmutableMap<Index, Curve> forwards = ImmutableMap.of(USD_LIBOR_3M, ConstantCurve.of(CURVE_ID_FWD.getCurveName(), 1));
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
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
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
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards =
        ImmutableMap.of(USD_FED_FUND, CURVE_ID_DSC, USD_LIBOR_3M, CURVE_ID_FWD, US_CPI_U, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    LocalDate valDate = date(2015, 6, 30);
    Curve dscCurve = ConstantCurve.of(Curves.discountFactors(CURVE_ID_DSC.getCurveName(), ACT_360), 1d);
    Curve fwdCurve = ConstantCurve.of(Curves.discountFactors(CURVE_ID_FWD.getCurveName(), ACT_360), 2d);
    MarketData md = ImmutableMarketData.of(valDate, ImmutableMap.of(CURVE_ID_DSC, dscCurve, CURVE_ID_FWD, fwdCurve));
    RatesProvider ratesProvider = test.ratesProvider(md);
    assertEquals(ratesProvider.getValuationDate(), valDate);
    assertEquals(ratesProvider.findData(CURVE_ID_DSC.getCurveName()), Optional.of(dscCurve));
    assertEquals(ratesProvider.findData(CURVE_ID_FWD.getCurveName()), Optional.of(fwdCurve));
    assertEquals(ratesProvider.findData(CurveName.of("Rubbish")), Optional.empty());
    // check discount factors
    SimpleDiscountFactors df = (SimpleDiscountFactors) ratesProvider.discountFactors(USD);
    assertEquals(df.getCurve().getName(), dscCurve.getName());
    assertThrowsIllegalArg(() -> ratesProvider.discountFactors(GBP));
    // check Ibor
    DiscountIborIndexRates ibor = (DiscountIborIndexRates) ratesProvider.iborIndexRates(USD_LIBOR_3M);
    SimpleDiscountFactors iborDf = (SimpleDiscountFactors) ibor.getDiscountFactors();
    assertEquals(iborDf.getCurve().getName(), fwdCurve.getName());
    assertThrowsIllegalArg(() -> ratesProvider.iborIndexRates(GBP_LIBOR_3M));
    // check Overnight
    DiscountOvernightIndexRates on = (DiscountOvernightIndexRates) ratesProvider.overnightIndexRates(USD_FED_FUND);
    SimpleDiscountFactors onDf = (SimpleDiscountFactors) on.getDiscountFactors();
    assertEquals(onDf.getCurve().getName(), dscCurve.getName());
    assertThrowsIllegalArg(() -> ratesProvider.overnightIndexRates(GBP_SONIA));
    // check price curve must be interpolated
    assertThrowsIllegalArg(() -> ratesProvider.priceIndexValues(US_CPI_U));
    // to immutable
    ImmutableRatesProvider expectedImmutable = ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(MarketDataFxRateProvider.of(md))
        .discountCurve(USD, dscCurve)
        .indexCurve(USD_FED_FUND, dscCurve)
        .indexCurve(USD_LIBOR_3M, fwdCurve)
        .indexCurve(US_CPI_U, fwdCurve)
        .build();
    assertEquals(ratesProvider.toImmutableRatesProvider(), expectedImmutable);
  }

  public void test_fxProvider() {
    RatesMarketDataLookup test = RatesMarketDataLookup.of(ImmutableMap.of(), ImmutableMap.of());
    LocalDate valDate = date(2015, 6, 30);
    FxRateId gbpUsdId = FxRateId.of(GBP, USD);
    FxRate gbpUsdRate = FxRate.of(GBP, USD, 1.6);
    MarketData md = ImmutableMarketData.of(valDate, ImmutableMap.of(gbpUsdId, gbpUsdRate));
    FxRateProvider fxProvider = test.fxRateProvider(md);
    assertEquals(fxProvider.fxRate(GBP, USD), 1.6);
    assertEquals(test.marketDataView(md).fxRateProvider().fxRate(GBP, USD), 1.6);
    assertThrows(() -> fxProvider.fxRate(EUR, USD), MarketDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test =
        DefaultRatesMarketDataLookup.of(discounts, forwards, ObservableSource.NONE, FxRateLookup.ofRates());
    coverImmutableBean(test);

    ImmutableMap<Currency, CurveId> discounts2 = ImmutableMap.of(GBP, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards2 = ImmutableMap.of(GBP_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test2 =
        DefaultRatesMarketDataLookup.of(discounts2, forwards2, OBS_SOURCE, FxRateLookup.ofRates(EUR));
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
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test =
        DefaultRatesMarketDataLookup.of(discounts, forwards, ObservableSource.NONE, FxRateLookup.ofRates());
    assertSerialization(test);
    Curve curve = ConstantCurve.of(Curves.discountFactors("DSC", ACT_360), 0.99);
    Map<? extends MarketDataId<?>, ?> valuesMap = ImmutableMap.of(
        CURVE_ID_DSC, curve, CURVE_ID_FWD, curve);
    MarketData md = MarketData.of(date(2016, 6, 30), valuesMap);
    assertSerialization(test.marketDataView(md));
    assertSerialization(test.ratesProvider(md));
  }

}
