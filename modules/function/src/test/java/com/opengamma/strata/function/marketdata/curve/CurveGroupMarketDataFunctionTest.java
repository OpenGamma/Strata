/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.jooq.lambda.Seq;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.function.interpolator.CurveExtrapolators;
import com.opengamma.strata.function.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.config.CurveGroupConfig;
import com.opengamma.strata.market.curve.config.CurveNode;
import com.opengamma.strata.market.curve.config.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.config.FraCurveNode;
import com.opengamma.strata.market.curve.config.InterpolatedCurveConfig;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.ParRatesId;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.RateIndexCurveKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapTradePricer;

@Test
public class CurveGroupMarketDataFunctionTest {

  /** The maximum allowable PV when round-tripping an instrument used to calibrate a curve. */
  private static final double PV_TOLERANCE = 5e-10;

  /**
   * Tests calibration a curve containing FRAs and pricing the curve instruments using the curve.
   */
  public void roundTripFra() {
    InterpolatedCurveConfig curveConfig = CurveTestUtils.fraCurveConfig();

    List<FraCurveNode> nodes = curveConfig.getNodes().stream()
        .map(FraCurveNode.class::cast)
        .collect(toImmutableList());

    List<ObservableId> ids = nodes.stream().map(CurveTestUtils::id).collect(toImmutableList());
    Map<ObservableId, Double> parRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(ids.get(0), 0.003)
        .put(ids.get(1), 0.0033)
        .put(ids.get(2), 0.0037)
        .put(ids.get(3), 0.0054)
        .put(ids.get(4), 0.007)
        .put(ids.get(5), 0.0091)
        .put(ids.get(6), 0.0134)
        .build();

    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA Curve");
    ParRates parRates = ParRates.of(parRateData, CurveMetadata.of(curveName));

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
    nodes.stream().forEach(node -> checkFraPvIsZero(node, valuationDate, ratesProvider, quotesMap));
  }

  public void roundTripFraAndFixedFloatSwap() {
    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    InterpolatedCurveConfig curveConfig = CurveTestUtils.fraSwapCurveConfig();
    CurveName curveName = curveConfig.getName();
    List<CurveNode> nodes = curveConfig.getNodes();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(groupName)
        .addCurve(curveConfig, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction(RootFinderConfig.defaults());
    LocalDate valuationDate = date(2011, 3, 8);

    Map<ObservableId, Double> parRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(CurveTestUtils.id(nodes.get(0)), 0.0037)
        .put(CurveTestUtils.id(nodes.get(1)), 0.0054)
        .put(CurveTestUtils.id(nodes.get(2)), 0.005)
        .put(CurveTestUtils.id(nodes.get(3)), 0.0087)
        .put(CurveTestUtils.id(nodes.get(4)), 0.012)
        .build();

    ParRates parRates = ParRates.of(parRateData, CurveMetadata.of(curveName));
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

    checkFraPvIsZero((FraCurveNode) nodes.get(0), valuationDate, ratesProvider, quotesMap);
    checkFraPvIsZero((FraCurveNode) nodes.get(1), valuationDate, ratesProvider, quotesMap);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(2), valuationDate, ratesProvider, quotesMap);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(3), valuationDate, ratesProvider, quotesMap);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(4), valuationDate, ratesProvider, quotesMap);
  }

  /**
   * Tests that par rates are required for curves.
   */
  public void requirements() {
    FraCurveNode node1x4   = CurveTestUtils.fraNode(1, "foo");
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

  public void metadata() {
    CurveGroupName groupName = CurveGroupName.of("Curve Group");

    InterpolatedCurveConfig fraSwapCurveConfig = CurveTestUtils.fraSwapCurveConfig();
    List<CurveNode> fraSwapNodes = fraSwapCurveConfig.getNodes();

    InterpolatedCurveConfig fraCurveConfig = CurveTestUtils.fraCurveConfig();
    List<CurveNode> fraNodes = fraCurveConfig.getNodes();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(groupName)
        .addDiscountingCurve(fraSwapCurveConfig, Currency.USD)
        .addForwardCurve(fraCurveConfig, IborIndices.USD_LIBOR_3M)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupName, groupConfig)
        .build();

    CurveGroupId curveGroupId = CurveGroupId.of(groupName);

    Map<ObservableId, Double> fraParRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(CurveTestUtils.id(fraNodes.get(0)), 0.003)
        .put(CurveTestUtils.id(fraNodes.get(1)), 0.0033)
        .put(CurveTestUtils.id(fraNodes.get(2)), 0.0037)
        .put(CurveTestUtils.id(fraNodes.get(3)), 0.0054)
        .put(CurveTestUtils.id(fraNodes.get(4)), 0.007)
        .put(CurveTestUtils.id(fraNodes.get(5)), 0.0091)
        .put(CurveTestUtils.id(fraNodes.get(6)), 0.0134).build();

    Map<ObservableId, Double> fraSwapParRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(CurveTestUtils.id(fraSwapNodes.get(0)), 0.0037)
        .put(CurveTestUtils.id(fraSwapNodes.get(1)), 0.0054)
        .put(CurveTestUtils.id(fraSwapNodes.get(2)), 0.005)
        .put(CurveTestUtils.id(fraSwapNodes.get(3)), 0.0087)
        .put(CurveTestUtils.id(fraSwapNodes.get(4)), 0.012).build();

    LocalDate valuationDate = date(2011, 3, 8);
    ParRates fraParRates = ParRates.of(fraParRateData, fraCurveConfig.metadata(valuationDate));
    ParRates fraSwapParRates = ParRates.of(fraSwapParRateData, fraCurveConfig.metadata(valuationDate));
    BaseMarketData marketData = BaseMarketData.builder(valuationDate)
        .addValue(ParRatesId.of(groupName, fraCurveConfig.getName(), MarketDataFeed.NONE), fraParRates)
        .addValue(ParRatesId.of(groupName, fraSwapCurveConfig.getName(), MarketDataFeed.NONE), fraSwapParRates)
        .build();

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction(RootFinderConfig.defaults());
    Result<CurveGroup> result = function.build(curveGroupId, marketData, marketDataConfig);

    assertThat(result).isSuccess();
    CurveGroup curveGroup = result.getValue();

    // Check the FRA/Swap curve identifiers are the expected tenors
    Curve discountCurve = curveGroup.getDiscountCurve(Currency.USD).get();
    List<CurveParameterMetadata> discountMetadata = discountCurve.getMetadata().getParameters().get();

    List<Object> discountTenors = discountMetadata.stream()
        .map(CurveParameterMetadata::getIdentifier)
        .collect(toImmutableList());

    List<Tenor> expectedDiscountTenors =
        ImmutableList.of(Tenor.TENOR_6M, Tenor.TENOR_9M, Tenor.TENOR_1Y, Tenor.TENOR_2Y, Tenor.TENOR_3Y);

    assertThat(discountTenors).isEqualTo(expectedDiscountTenors);

    List<CurveParameterMetadata> expectedDiscountMetadata = fraSwapNodes.stream()
        .map(node -> node.metadata(valuationDate))
        .collect(toImmutableList());

    assertThat(discountMetadata).isEqualTo(expectedDiscountMetadata);

    // Check the FRA curve identifiers are the expected tenors
    Curve forwardCurve = curveGroup.getForwardCurve(IborIndices.USD_LIBOR_3M).get();
    List<CurveParameterMetadata> forwardMetadata = forwardCurve.getMetadata().getParameters().get();

    List<Object> forwardTenors = forwardMetadata.stream()
        .map(CurveParameterMetadata::getIdentifier)
        .collect(toImmutableList());

    List<Tenor> expectedForwardTenors =
        ImmutableList.of(
            Tenor.TENOR_4M,
            Tenor.TENOR_5M,
            Tenor.TENOR_6M,
            Tenor.TENOR_9M,
            Tenor.TENOR_12M,
            Tenor.ofMonths(15),
            Tenor.ofMonths(21));

    assertThat(forwardTenors).isEqualTo(expectedForwardTenors);

    List<CurveParameterMetadata> expectedForwardMetadata = fraNodes.stream()
        .map(node -> node.metadata(valuationDate))
        .collect(toImmutableList());

    assertThat(forwardMetadata).isEqualTo(expectedForwardMetadata);
  }

  //-----------------------------------------------------------------------------------------------------------

  private void checkFraPvIsZero(
      FraCurveNode node,
      LocalDate valuationDate,
      RatesProvider ratesProvider,
      Map<ObservableKey, Double> marketDataMap) {

    Trade trade = node.trade(valuationDate, marketDataMap);
    CurrencyAmount currencyAmount = DiscountingFraTradePricer.DEFAULT.presentValue((FraTrade) trade, ratesProvider);
    double pv = currencyAmount.getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

  private void checkSwapPvIsZero(
      FixedIborSwapCurveNode node,
      LocalDate valuationDate,
      RatesProvider ratesProvider,
      Map<ObservableKey, Double> marketDataMap) {

    Trade trade = node.trade(valuationDate, marketDataMap);
    MultiCurrencyAmount amount = DiscountingSwapTradePricer.DEFAULT.presentValue((SwapTrade) trade, ratesProvider);
    double pv = amount.getAmount(Currency.USD).getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }
}
