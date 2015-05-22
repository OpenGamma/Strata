/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.fra.FraTemplate;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.function.interpolator.CurveExtrapolators;
import com.opengamma.strata.function.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.config.CurveGroupConfig;
import com.opengamma.strata.market.curve.config.CurveNode;
import com.opengamma.strata.market.curve.config.FraCurveNode;
import com.opengamma.strata.market.curve.config.InterpolatedCurveConfig;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.ParRatesId;
import com.opengamma.strata.market.key.DiscountingCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.key.RateIndexCurveKey;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraTradePricer;

@Test
public class CurveGroupMarketDataFunctionTest {

  /**
   * Tests calibration a curve containing FRAs and pricing the curve instruments using the curve.
   */
  public void roundTripFra() {
    FraCurveNode node1x4   = fraNodeFixed(1, 0.3);
    FraCurveNode node2x5   = fraNodeFixed(2, 0.33);
    FraCurveNode node3x6   = fraNodeFixed(3, 0.37);
    FraCurveNode node6x9   = fraNodeFixed(6, 0.54);
    FraCurveNode node9x12  = fraNodeFixed(9, 0.7);
    FraCurveNode node12x15 = fraNodeFixed(12, 0.91);
    FraCurveNode node18x21 = fraNodeFixed(18, 1.34);

    List<CurveNode> nodes = ImmutableList.of(node1x4, node2x5, node3x6, node6x9, node9x12, node12x15, node18x21);
    String groupName = "Curve Group";
    String curveName = "FRA Curve";

    InterpolatedCurveConfig curveConfig = InterpolatedCurveConfig.builder()
        .name(curveName)
        .nodes(nodes)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .leftExtrapolator(CurveExtrapolators.FLAT)
        .rightExtrapolator(CurveExtrapolators.FLAT)
        .build();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(groupName)
        .addCurve(curveConfig, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction(RootFinderConfig.defaults());
    LocalDate valuationDate = date(2011, 3, 8);
    BaseMarketData emptyMarketData = BaseMarketData.empty(valuationDate);
    Result<CurveGroup> result = function.buildCurveGroup(groupConfig, emptyMarketData, MarketDataFeed.NONE);

    assertThat(result).isSuccess();
    CurveGroup curveGroup = result.getValue();
    YieldAndDiscountCurve curve = curveGroup.getMulticurveProvider().getCurve(Currency.USD);

    DiscountingCurveKey discountingCurveKey = DiscountingCurveKey.of(Currency.USD);
    RateIndexCurveKey forwardCurveKey = RateIndexCurveKey.of(IborIndices.USD_LIBOR_3M);
    Map<MarketDataKey<?>, Object> marketDataMap = ImmutableMap.of(discountingCurveKey, curve, forwardCurveKey, curve);
    // TODO Is a time series actually necessary for FRAs? It's in the requirements so we have to provide it.
    Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries =
        ImmutableMap.of(IndexRateKey.of(IborIndices.USD_LIBOR_3M), LocalDateDoubleTimeSeries.empty());
    CalculationMarketData calculationMarketData = new MarketDataMap(valuationDate, marketDataMap, timeSeries);
    MarketDataRatesProvider ratesProvider =
        new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(calculationMarketData, 0));

    // The PV should be zero for an instrument used to build the curve
    checkFraPvIsZero(node1x4, valuationDate, ratesProvider);
    checkFraPvIsZero(node2x5, valuationDate, ratesProvider);
    checkFraPvIsZero(node3x6, valuationDate, ratesProvider);
    checkFraPvIsZero(node6x9, valuationDate, ratesProvider);
    checkFraPvIsZero(node9x12, valuationDate, ratesProvider);
    checkFraPvIsZero(node12x15, valuationDate, ratesProvider);
    checkFraPvIsZero(node18x21, valuationDate, ratesProvider);
  }

  /**
   * Tests that par rates are required for curves.
   */
  public void requirements() {
    FraCurveNode node1x4   = fraNodeMarket(1, "foo");

    List<CurveNode> nodes = ImmutableList.of(node1x4);
    String groupName = "Curve Group";
    String curveName = "FRA Curve";
    MarketDataFeed feed = MarketDataFeed.of("TestFeed");

    InterpolatedCurveConfig curveConfig = InterpolatedCurveConfig.builder()
        .name(curveName)
        .nodes(nodes)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .leftExtrapolator(CurveExtrapolators.FLAT)
        .rightExtrapolator(CurveExtrapolators.FLAT)
        .build();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(groupName)
        .addCurve(curveConfig, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupName, groupConfig)
        .build();

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction(RootFinderConfig.defaults());
    CurveGroupId curveGroupId = CurveGroupId.of(groupName, feed);
    MarketDataRequirements requirements = function.requirements(curveGroupId, marketDataConfig);

    assertThat(requirements.getNonObservables()).contains(ParRatesId.of(groupName, curveName, feed));
  }

  /**
   * Tests that no par rates are required if the curve config contains the market data.
   */
  public void noRequirementsIfCurveConfigContainsMarketData() {
    FraCurveNode node1x4   = fraNodeFixed(1, 0.3);
    List<CurveNode> nodes = ImmutableList.of(node1x4);
    String groupName = "Curve Group";
    String curveName = "FRA Curve";

    InterpolatedCurveConfig curveConfig = InterpolatedCurveConfig.builder()
        .name(curveName)
        .nodes(nodes)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .leftExtrapolator(CurveExtrapolators.FLAT)
        .rightExtrapolator(CurveExtrapolators.FLAT)
        .build();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(groupName)
        .addCurve(curveConfig, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupName, groupConfig)
        .build();

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction(RootFinderConfig.defaults());
    CurveGroupId curveGroupId = CurveGroupId.of(groupName);
    MarketDataRequirements requirements = function.requirements(curveGroupId, marketDataConfig);

    assertThat(requirements.getNonObservables()).isEmpty();
  }

  //-----------------------------------------------------------------------------------------------------------

  private void checkFraPvIsZero(FraCurveNode node, LocalDate valuationDate, RatesProvider ratesProvider) {
    Trade trade = node.buildTrade(valuationDate, ImmutableMap.of());
    CurrencyAmount currencyAmount = DiscountingFraTradePricer.DEFAULT.presentValue((FraTrade) trade, ratesProvider);
    double pv = currencyAmount.getAmount();
    assertThat(pv).isCloseTo(0, offset(1e-6));
  }

  private static FraCurveNode fraNodeFixed(int startTenor, double rate) {
    Period periodToStart = Period.ofMonths(startTenor);
    return FraCurveNode.ofFixedRate(FraTemplate.of(periodToStart, IborIndices.USD_LIBOR_3M), rate / 100);
  }

  private static FraCurveNode fraNodeMarket(int startTenor, String id) {
    Period periodToStart = Period.ofMonths(startTenor);
    QuoteKey quoteKey = QuoteKey.of(StandardId.of("test", id));
    return FraCurveNode.ofMarketRate(FraTemplate.of(periodToStart, IborIndices.USD_LIBOR_3M), quoteKey);
  }

  //-----------------------------------------------------------------------------------------------------------

  private static final class MarketDataMap implements CalculationMarketData {

    private final LocalDate valuationDate;

    private final Map<MarketDataKey<?>, Object> marketData;

    private final Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap;

    private MarketDataMap(
        LocalDate valuationDate,
        Map<MarketDataKey<?>, Object> marketData,
        Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap) {

      this.valuationDate = valuationDate;
      this.marketData = marketData;
      this.timeSeriesMap = timeSeriesMap;
    }

    @Override
    public List<LocalDate> getValuationDates() {
      return ImmutableList.of(valuationDate);
    }

    @Override
    public int getScenarioCount() {
      return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, K extends MarketDataKey<T>> List<T> getValues(K key) {
      T value = (T) marketData.get(key);

      if (value != null) {
        return ImmutableList.of(value);
      } else {
        throw new IllegalArgumentException("No market data for " + key);
      }
    }

    @Override
    public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
      LocalDateDoubleTimeSeries timeSeries = timeSeriesMap.get(key);

      if (timeSeries != null) {
        return timeSeries;
      } else {
        throw new IllegalArgumentException("No time series for " + key);
      }
    }

    @Override
    public <T, K extends MarketDataKey<T>> T getGlobalValue(K key) {
      throw new UnsupportedOperationException("getGlobalValue not implemented");
    }
  }
}
