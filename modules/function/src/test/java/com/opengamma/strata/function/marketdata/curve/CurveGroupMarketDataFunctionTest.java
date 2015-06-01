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

import org.jooq.lambda.Seq;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.schedule.Frequency;
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
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.finance.rate.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.finance.rate.swap.type.IborRateSwapLegConvention;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.function.interpolator.CurveExtrapolators;
import com.opengamma.strata.function.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.config.CurveGroupConfig;
import com.opengamma.strata.market.curve.config.CurveNode;
import com.opengamma.strata.market.curve.config.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.config.FraCurveNode;
import com.opengamma.strata.market.curve.config.InterpolatedCurveConfig;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.ParRatesId;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.key.RateIndexCurveKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapTradePricer;

@Test
public class CurveGroupMarketDataFunctionTest {

  private static final String TEST_SCHEME = "test";
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(
      BusinessDayConventions.FOLLOWING,
      HolidayCalendars.GBLO);

  private static final IborRateSwapLegConvention FLOATING_CONVENTION =
      IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M);

  private static final FixedRateSwapLegConvention FIXED_CONVENTION =
      FixedRateSwapLegConvention.of(Currency.USD, DayCounts.ACT_360, Frequency.P6M, BDA_FOLLOW);

  private static final FixedIborSwapConvention SWAP_CONVENTION =
      FixedIborSwapConvention.of(FIXED_CONVENTION, FLOATING_CONVENTION);

  /** The maximum allowable PV when round-tripping an instrument used to calibrate a curve. */
  private static final double PV_TOLERANCE = 5e-10;


  /**
   * Tests calibration a curve containing FRAs and pricing the curve instruments using the curve.
   */
  public void roundTripFra() {
    String fra1x4 = "fra1x4";
    String fra2x5 = "fra2x5";
    String fra3x6 = "fra3x6";
    String fra6x9 = "fra6x9";
    String fra9x12 = "fra9x12";
    String fra12x15 = "fra12x15";
    String fra18x21 = "fra18x21";

    FraCurveNode fra1x4Node   = fraNode(1, fra1x4);
    FraCurveNode fra2x5Node   = fraNode(2, fra2x5);
    FraCurveNode fra3x6Node   = fraNode(3, fra3x6);
    FraCurveNode fra6x9Node   = fraNode(6, fra6x9);
    FraCurveNode fra9x12Node  = fraNode(9, fra9x12);
    FraCurveNode fra12x15Node = fraNode(12, fra12x15);
    FraCurveNode fra18x21Node = fraNode(18, fra18x21);

    Map<ObservableId, Double> parRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(id(fra1x4),   0.003)
        .put(id(fra2x5),   0.0033)
        .put(id(fra3x6),   0.0037)
        .put(id(fra6x9),   0.0054)
        .put(id(fra9x12),  0.007)
        .put(id(fra12x15), 0.0091)
        .put(id(fra18x21), 0.0134)
        .build();

    ParRates parRates = ParRates.of(parRateData);

    List<CurveNode> nodes =
        ImmutableList.of(fra1x4Node, fra2x5Node, fra3x6Node, fra6x9Node, fra9x12Node, fra12x15Node, fra18x21Node);

    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA Curve");

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
    BaseMarketData marketData = BaseMarketData.builder(valuationDate)
        .addValue(ParRatesId.of(groupName, curveName, MarketDataFeed.NONE), parRates)
        .build();
    Result<CurveGroup> result = function.buildCurveGroup(groupConfig, marketData, MarketDataFeed.NONE);

    assertThat(result).isSuccess();
    CurveGroup curveGroup = result.getValue();
    Curve curve = curveGroup.getDiscountCurve(Currency.USD).get();

    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(Currency.USD);
    RateIndexCurveKey forwardCurveKey = RateIndexCurveKey.of(IborIndices.USD_LIBOR_3M);
    Map<ObservableKey, Double> quotesMap = Seq.seq(parRateData).toMap(tp -> tp.v1.toObservableKey(), tp -> tp.v2);
    DiscountFactors discountFactors =
        ZeroRateDiscountFactors.of(Currency.USD, valuationDate, DayCounts.ACT_ACT_ISDA, curve);
    Map<MarketDataKey<?>, Object> marketDataMap = ImmutableMap.<MarketDataKey<?>, Object>builder()
        .putAll(quotesMap)
        .put(discountFactorsKey, discountFactors)
        .put(forwardCurveKey, curve)
        .build();
    Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries =
        ImmutableMap.of(IndexRateKey.of(IborIndices.USD_LIBOR_3M), LocalDateDoubleTimeSeries.empty());
    CalculationMarketData calculationMarketData = new MarketDataMap(valuationDate, marketDataMap, timeSeries);
    MarketDataRatesProvider ratesProvider =
        new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(calculationMarketData, 0));

    // The PV should be zero for an instrument used to build the curve
    checkFraPvIsZero(fra1x4Node, valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero(fra2x5Node, valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero(fra3x6Node, valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero(fra6x9Node, valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero(fra9x12Node, valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero(fra12x15Node, valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero(fra18x21Node, valuationDate, ratesProvider, quotesMap);
  }

  public void roundTripFraAndFixedFloatSwap() {
    String fra3x6 = "fra3x6";
    String fra6x9 = "fra6x9";
    String swap1y = "swap1y";
    String swap2y = "swap2y";
    String swap3y = "swap3y";

    FraCurveNode fra3x6Node = fraNode(3, fra3x6);
    FraCurveNode fra6x9Node = fraNode(6, fra6x9);
    FixedIborSwapCurveNode swap1yNode = fixedIborSwapNode(Tenor.TENOR_1Y, swap1y);
    FixedIborSwapCurveNode swap2yNode = fixedIborSwapNode(Tenor.TENOR_2Y, swap2y);
    FixedIborSwapCurveNode swap3yNode = fixedIborSwapNode(Tenor.TENOR_3Y, swap3y);

    List<CurveNode> nodes = ImmutableList.of(fra3x6Node, fra6x9Node, swap1yNode, swap2yNode, swap3yNode);
    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA and Fixed-Float Swap Curve");

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

    Map<ObservableId, Double> parRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(id(fra3x6), 0.0037)
        .put(id(fra6x9), 0.0054)
        .put(id(swap1y), 0.005)
        .put(id(swap2y), 0.0087)
        .put(id(swap3y), 0.012)
        .build();

    ParRates parRates = ParRates.of(parRateData);
    BaseMarketData marketData = BaseMarketData.builder(valuationDate)
        .addValue(ParRatesId.of(groupName, curveName, MarketDataFeed.NONE), parRates)
        .build();

    Result<CurveGroup> result = function.buildCurveGroup(groupConfig, marketData, MarketDataFeed.NONE);
    assertThat(result).isSuccess();
    CurveGroup curveGroup = result.getValue();
    Curve curve = curveGroup.getDiscountCurve(Currency.USD).get();
    DiscountFactors discountFactors =
        ZeroRateDiscountFactors.of(Currency.USD, valuationDate, DayCounts.ACT_ACT_ISDA, curve);

    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(Currency.USD);
    RateIndexCurveKey forwardCurveKey = RateIndexCurveKey.of(IborIndices.USD_LIBOR_3M);
    Map<ObservableKey, Double> quotesMap = Seq.seq(parRateData).toMap(tp -> tp.v1.toObservableKey(), tp -> tp.v2);
    Map<MarketDataKey<?>, Object> marketDataMap = ImmutableMap.<MarketDataKey<?>, Object>builder()
        .putAll(quotesMap)
        .put(discountFactorsKey, discountFactors)
        .put(forwardCurveKey, curve)
        .build();
    Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries =
        ImmutableMap.of(IndexRateKey.of(IborIndices.USD_LIBOR_3M), LocalDateDoubleTimeSeries.empty());
    CalculationMarketData calculationMarketData = new MarketDataMap(valuationDate, marketDataMap, timeSeries);
    MarketDataRatesProvider ratesProvider =
        new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(calculationMarketData, 0));

    checkFraPvIsZero(fra3x6Node, valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero(fra6x9Node, valuationDate, ratesProvider, quotesMap);
    checkSwapPvIsZero(swap1yNode, valuationDate, ratesProvider, quotesMap);
    checkSwapPvIsZero(swap2yNode, valuationDate, ratesProvider, quotesMap);
    checkSwapPvIsZero(swap3yNode, valuationDate, ratesProvider, quotesMap);
  }

  private static ObservableId id(String idValue) {
    return QuoteId.of(StandardId.of(TEST_SCHEME, idValue));
  }

  /**
   * Tests that par rates are required for curves.
   */
  public void requirements() {
    FraCurveNode node1x4   = fraNode(1, "foo");

    List<CurveNode> nodes = ImmutableList.of(node1x4);
    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA Curve");
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

  //-----------------------------------------------------------------------------------------------------------

  private void checkFraPvIsZero(
      FraCurveNode node,
      LocalDate valuationDate,
      RatesProvider ratesProvider,
      Map<ObservableKey, Double> marketDataMap) {

    Trade trade = node.buildTrade(valuationDate, marketDataMap);
    CurrencyAmount currencyAmount = DiscountingFraTradePricer.DEFAULT.presentValue((FraTrade) trade, ratesProvider);
    double pv = currencyAmount.getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

  private void checkSwapPvIsZero(
      FixedIborSwapCurveNode node,
      LocalDate valuationDate,
      RatesProvider ratesProvider,
      Map<ObservableKey, Double> marketDataMap) {

    Trade trade = node.buildTrade(valuationDate, marketDataMap);
    MultiCurrencyAmount amount = DiscountingSwapTradePricer.DEFAULT.presentValue((SwapTrade) trade, ratesProvider);
    double pv = amount.getAmount(Currency.USD).getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

  private static FraCurveNode fraNode(int startMonths, String id) {
    Period periodToStart = Period.ofMonths(startMonths);
    QuoteKey quoteKey = QuoteKey.of(StandardId.of(TEST_SCHEME, id));
    return FraCurveNode.of(FraTemplate.of(periodToStart, IborIndices.USD_LIBOR_3M), quoteKey);
  }

  private static FixedIborSwapCurveNode fixedIborSwapNode(Tenor tenor, String id) {
    QuoteKey quoteKey = QuoteKey.of(StandardId.of(TEST_SCHEME, id));
    FixedIborSwapTemplate template = FixedIborSwapTemplate.of(Period.ZERO, tenor, SWAP_CONVENTION);
    return FixedIborSwapCurveNode.of(template, quoteKey);
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
    public <T> List<T> getValues(MarketDataKey<T> key) {
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
