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
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.joda.beans.ser.JodaBeanSer;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.calc.runner.FxRateLookup;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
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
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.RatesCurveGroup;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupEntry;
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
public class RatesMarketDataLookupTest {

  private static final IborIndex INACTIVE_IBOR_INDEX = IborIndex.of("GBP-LIBOR-10M");
  private static final OvernightIndex INACTIVE_ON_INDEX = OvernightIndex.of("CHF-TOIS");
  private static final CurveId CURVE_ID_DSC = CurveId.of("Group", "USD-DSC");
  private static final CurveId CURVE_ID_FWD = CurveId.of("Group", "USD-L3M");
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);

  //-------------------------------------------------------------------------
  @Test
  @SuppressWarnings("deprecation")
  public void test_of_map() {
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    assertThat(test.queryType()).isEqualTo(RatesMarketDataLookup.class);
    assertThat(test.getDiscountCurrencies()).containsOnly(USD);
    assertThat(test.getDiscountMarketDataIds(USD)).containsOnly(CURVE_ID_DSC);
    assertThat(test.getForwardIndices()).containsOnly(USD_LIBOR_3M);
    assertThat(test.getForwardMarketDataIds(USD_LIBOR_3M)).containsOnly(CURVE_ID_FWD);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getDiscountMarketDataIds(GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getForwardMarketDataIds(GBP_LIBOR_3M));
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getFxRateLookup()).isEqualTo(FxRateLookup.ofRates());

    assertThat(test.requirements(USD))
        .isEqualTo(FunctionRequirements.builder().valueRequirements(CURVE_ID_DSC).outputCurrencies(USD).build());
    assertThat(test.requirements(USD, USD_LIBOR_3M, EUR_EURIBOR_2M))
        .isEqualTo(FunctionRequirements.builder()
            .valueRequirements(CURVE_ID_DSC, CURVE_ID_FWD)
            .timeSeriesRequirements(IndexQuoteId.of(USD_LIBOR_3M), IndexQuoteId.of(EUR_EURIBOR_2M))
            .outputCurrencies(USD)
            .build());
    assertThat(test.requirements(ImmutableSet.of(USD), ImmutableSet.of(USD_LIBOR_3M)))
        .isEqualTo(FunctionRequirements.builder()
            .valueRequirements(CURVE_ID_DSC, CURVE_ID_FWD)
            .timeSeriesRequirements(IndexQuoteId.of(USD_LIBOR_3M))
            .outputCurrencies(USD)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.requirements(ImmutableSet.of(USD), ImmutableSet.of(GBP_LIBOR_3M)));

    assertThat(test.ratesProvider(MOCK_MARKET_DATA))
        .isEqualTo(DefaultLookupRatesProvider.of((DefaultRatesMarketDataLookup) test, MOCK_MARKET_DATA));
  }

  @Test
  public void test_of_groupNameAndMap() {
    ImmutableMap<Currency, CurveName> discounts = ImmutableMap.of(USD, CURVE_ID_DSC.getCurveName());
    ImmutableMap<Index, CurveName> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD.getCurveName());
    RatesMarketDataLookup test = RatesMarketDataLookup.of(CURVE_ID_DSC.getCurveGroupName(), discounts, forwards);
    assertThat(test.queryType()).isEqualTo(RatesMarketDataLookup.class);
    assertThat(test.getDiscountCurrencies()).containsOnly(USD);
    assertThat(test.getDiscountMarketDataIds(USD)).containsOnly(CURVE_ID_DSC);
    assertThat(test.getForwardIndices()).containsOnly(USD_LIBOR_3M);
    assertThat(test.getForwardMarketDataIds(USD_LIBOR_3M)).containsOnly(CURVE_ID_FWD);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getDiscountMarketDataIds(GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getForwardMarketDataIds(GBP_LIBOR_3M));
  }

  @Test
  public void test_of_curveGroup() {
    ImmutableMap<Currency, Curve> discounts = ImmutableMap.of(USD, ConstantCurve.of(CURVE_ID_DSC.getCurveName(), 1));
    ImmutableMap<Index, Curve> forwards = ImmutableMap.of(USD_LIBOR_3M, ConstantCurve.of(CURVE_ID_FWD.getCurveName(), 1));
    RatesCurveGroup group = RatesCurveGroup.of(CURVE_ID_DSC.getCurveGroupName(), discounts, forwards);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(group);
    assertThat(test.queryType()).isEqualTo(RatesMarketDataLookup.class);
    assertThat(test.getDiscountCurrencies()).containsOnly(USD);
    assertThat(test.getDiscountMarketDataIds(USD)).containsOnly(CURVE_ID_DSC);
    assertThat(test.getForwardIndices()).containsOnly(USD_LIBOR_3M);
    assertThat(test.getForwardMarketDataIds(USD_LIBOR_3M)).containsOnly(CURVE_ID_FWD);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getDiscountMarketDataIds(GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getForwardMarketDataIds(GBP_LIBOR_3M));
  }

  @Test
  public void test_of_curveGroupDefinition_and_observableSource() {
    RatesCurveGroupEntry entry1 = RatesCurveGroupEntry.builder()
        .curveName(CURVE_ID_DSC.getCurveName())
        .discountCurrencies(USD)
        .build();

    RatesCurveGroupEntry entry2 = RatesCurveGroupEntry.builder()
        .curveName(CURVE_ID_FWD.getCurveName())
        .indices(USD_LIBOR_3M)
        .build();

    List<RatesCurveGroupEntry> entries = ImmutableList.of(entry1, entry2);
    CurveGroupName groupName = CURVE_ID_DSC.getCurveGroupName();
    RatesCurveGroupDefinition groupDefinition = RatesCurveGroupDefinition.of(groupName, entries, ImmutableList.of());

    // The lookup should contain curve IDs with the non-default ObservableSource
    CurveId dscId = CurveId.of(CURVE_ID_DSC.getCurveGroupName(), CURVE_ID_DSC.getCurveName(), OBS_SOURCE);
    CurveId fwdId = CurveId.of(CURVE_ID_FWD.getCurveGroupName(), CURVE_ID_FWD.getCurveName(), OBS_SOURCE);

    RatesMarketDataLookup test = RatesMarketDataLookup.of(groupDefinition, OBS_SOURCE, FxRateLookup.ofRates());
    assertThat(test.queryType()).isEqualTo(RatesMarketDataLookup.class);
    assertThat(test.getDiscountCurrencies()).containsOnly(USD);
    assertThat(test.getDiscountMarketDataIds(USD)).containsOnly(dscId);
    assertThat(test.getForwardIndices()).containsOnly(USD_LIBOR_3M);
    assertThat(test.getForwardMarketDataIds(USD_LIBOR_3M)).containsOnly(fwdId);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getDiscountMarketDataIds(GBP));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getForwardMarketDataIds(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_marketDataView() {
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    RatesScenarioMarketData multiScenario = test.marketDataView(md);
    assertThat(multiScenario.getLookup()).isEqualTo(test);
    assertThat(multiScenario.getMarketData()).isEqualTo(md);
    assertThat(multiScenario.getScenarioCount()).isEqualTo(1);
    RatesMarketData scenario = multiScenario.scenario(0);
    assertThat(scenario.getLookup()).isEqualTo(test);
    assertThat(scenario.getMarketData()).isEqualTo(md.scenario(0));
    assertThat(scenario.getValuationDate()).isEqualTo(valDate);
  }

  @Test
  public void test_ratesProvider() {
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards =
        ImmutableMap.of(USD_FED_FUND, CURVE_ID_DSC, USD_LIBOR_3M, CURVE_ID_FWD, US_CPI_U, CURVE_ID_FWD);
    RatesMarketDataLookup test = RatesMarketDataLookup.of(discounts, forwards);
    LocalDate valDate = date(2015, 6, 30);
    Curve dscCurve = ConstantCurve.of(Curves.discountFactors(CURVE_ID_DSC.getCurveName(), ACT_360), 1d);
    Curve fwdCurve = ConstantCurve.of(Curves.discountFactors(CURVE_ID_FWD.getCurveName(), ACT_360), 2d);
    LocalDateDoubleTimeSeries dummyTimeSeries = LocalDateDoubleTimeSeries.of(valDate, 1);
    MarketData md = ImmutableMarketData.builder(valDate)
        .addValue(CURVE_ID_DSC, dscCurve)
        .addValue(CURVE_ID_FWD, fwdCurve)
        .addTimeSeries(IndexQuoteId.of(INACTIVE_IBOR_INDEX), dummyTimeSeries)
        .addTimeSeries(IndexQuoteId.of(INACTIVE_ON_INDEX), dummyTimeSeries)
        .build();
    RatesProvider ratesProvider = test.ratesProvider(md);
    assertThat(ratesProvider.getValuationDate()).isEqualTo(valDate);
    assertThat(ratesProvider.findData(CURVE_ID_DSC.getCurveName())).isEqualTo(Optional.of(dscCurve));
    assertThat(ratesProvider.findData(CURVE_ID_FWD.getCurveName())).isEqualTo(Optional.of(fwdCurve));
    assertThat(ratesProvider.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(ratesProvider.getIborIndices()).containsOnly(USD_LIBOR_3M);
    assertThat(ratesProvider.getOvernightIndices()).containsOnly(USD_FED_FUND);
    assertThat(ratesProvider.getPriceIndices()).containsOnly(US_CPI_U);
    assertThat(ratesProvider.getTimeSeriesIndices()).containsOnly(INACTIVE_IBOR_INDEX, INACTIVE_ON_INDEX);
    // check discount factors
    SimpleDiscountFactors df = (SimpleDiscountFactors) ratesProvider.discountFactors(USD);
    assertThat(df.getCurve().getName()).isEqualTo(dscCurve.getName());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ratesProvider.discountFactors(GBP));
    // check Ibor
    DiscountIborIndexRates ibor = (DiscountIborIndexRates) ratesProvider.iborIndexRates(USD_LIBOR_3M);
    SimpleDiscountFactors iborDf = (SimpleDiscountFactors) ibor.getDiscountFactors();
    assertThat(iborDf.getCurve().getName()).isEqualTo(fwdCurve.getName());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ratesProvider.iborIndexRates(GBP_LIBOR_3M));
    assertThat(ratesProvider.iborIndexRates(INACTIVE_IBOR_INDEX).getIndex()).isEqualTo(INACTIVE_IBOR_INDEX);
    assertThat(ratesProvider.iborIndexRates(INACTIVE_IBOR_INDEX).getFixings()).isEqualTo(dummyTimeSeries);
    // check Overnight
    DiscountOvernightIndexRates on = (DiscountOvernightIndexRates) ratesProvider.overnightIndexRates(USD_FED_FUND);
    SimpleDiscountFactors onDf = (SimpleDiscountFactors) on.getDiscountFactors();
    assertThat(onDf.getCurve().getName()).isEqualTo(dscCurve.getName());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ratesProvider.overnightIndexRates(GBP_SONIA));
    assertThat(ratesProvider.overnightIndexRates(INACTIVE_ON_INDEX).getIndex()).isEqualTo(INACTIVE_ON_INDEX);
    assertThat(ratesProvider.overnightIndexRates(INACTIVE_ON_INDEX).getFixings()).isEqualTo(dummyTimeSeries);
    // check price curve must be interpolated
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ratesProvider.priceIndexValues(US_CPI_U));
    // to immutable
    ImmutableRatesProvider expectedImmutable = ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(MarketDataFxRateProvider.of(md))
        .discountCurve(USD, dscCurve)
        .indexCurve(USD_FED_FUND, dscCurve)
        .indexCurve(USD_LIBOR_3M, fwdCurve)
        .indexCurve(US_CPI_U, fwdCurve)
        .timeSeries(INACTIVE_IBOR_INDEX, dummyTimeSeries)
        .timeSeries(INACTIVE_ON_INDEX, dummyTimeSeries)
        .build();
    assertThat(ratesProvider.toImmutableRatesProvider()).isEqualTo(expectedImmutable);
  }

  @Test
  public void test_fxProvider() {
    RatesMarketDataLookup test = RatesMarketDataLookup.of(ImmutableMap.of(), ImmutableMap.of());
    LocalDate valDate = date(2015, 6, 30);
    FxRateId gbpUsdId = FxRateId.of(GBP, USD);
    FxRate gbpUsdRate = FxRate.of(GBP, USD, 1.6);
    MarketData md = ImmutableMarketData.of(valDate, ImmutableMap.of(gbpUsdId, gbpUsdRate));
    FxRateProvider fxProvider = test.fxRateProvider(md);
    assertThat(fxProvider.fxRate(GBP, USD)).isEqualTo(1.6);
    assertThat(test.marketDataView(md).fxRateProvider().fxRate(GBP, USD)).isEqualTo(1.6);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> fxProvider.fxRate(EUR, USD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test =
        DefaultRatesMarketDataLookup.of(discounts, forwards, ObservableSource.NONE, FxRateLookup.ofRates());
    coverImmutableBean(test);

    ImmutableMap<Currency, CurveId> discounts2 = ImmutableMap.of(GBP, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards2 = ImmutableMap.of(GBP_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test2 =
        DefaultRatesMarketDataLookup.of(discounts2, forwards2, ObservableSource.NONE, FxRateLookup.ofRates(EUR));
    coverBeanEquals(test, test2);

    // related coverage
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    DefaultRatesScenarioMarketData.meta();

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
    DefaultRatesMarketData.meta();

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA).ratesProvider());
    DefaultLookupRatesProvider.meta();
  }

  @Test
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

  @Test
  public void test_jodaSerialization() {
    ImmutableMap<Currency, CurveId> discounts = ImmutableMap.of(USD, CURVE_ID_DSC);
    ImmutableMap<Index, CurveId> forwards = ImmutableMap.of(USD_LIBOR_3M, CURVE_ID_FWD);
    DefaultRatesMarketDataLookup test =
        DefaultRatesMarketDataLookup.of(discounts, forwards, ObservableSource.NONE, FxRateLookup.ofRates());
    String xml = JodaBeanSer.PRETTY.xmlWriter().write(test);
    assertThat(xml.contains("<entry key=\"USD-LIBOR-3M\">")).isTrue();
    assertThat(xml.contains("<fixingDateOffset>")).isFalse();
    assertThat(xml.contains("<effectiveDateOffset>")).isFalse();
  }

}
