/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.calculation.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.MarketEnvironment;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.interpolator.CurveExtrapolators;
import com.opengamma.strata.function.interpolator.CurveInterpolators;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveNode;
import com.opengamma.strata.market.curve.definition.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.definition.FraCurveNode;
import com.opengamma.strata.market.curve.definition.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.ParRatesId;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IborIndexRatesKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.DiscountIborIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapTradePricer;

/**
 * Test {@link CurveGroupMarketDataFunction}.
 */
@Test
public class CurveGroupMarketDataFunctionTest {

  /** The maximum allowable PV when round-tripping an instrument used to calibrate a curve. */
  private static final double PV_TOLERANCE = 5e-10;

  /**
   * Tests calibration a curve containing FRAs and pricing the curve instruments using the curve.
   */
  public void roundTripFra() {
    InterpolatedNodalCurveDefinition curveDefn = CurveTestUtils.fraCurveDefinition();

    List<FraCurveNode> nodes = curveDefn.getNodes().stream()
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
    ParRates parRates = ParRates.of(parRateData, DefaultCurveMetadata.of(curveName));

    CurveGroupDefinition groupDefn = CurveGroupDefinition.builder()
        .name(groupName)
        .addCurve(curveDefn, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    CurveGroupMarketDataFunction function =
        new CurveGroupMarketDataFunction(RootFinderConfig.defaults(), CalibrationMeasures.DEFAULT);
    LocalDate valuationDate = date(2011, 3, 8);
    MarketEnvironment marketData = MarketEnvironment.builder(valuationDate)
        .addValue(ParRatesId.of(groupName, curveName, MarketDataFeed.NONE), parRates)
        .build();
    Result<CurveGroup> result = function.buildCurveGroup(groupDefn, marketData, MarketDataFeed.NONE);

    assertThat(result).isSuccess();
    CurveGroup curveGroup = result.getValue();
    Curve curve = curveGroup.findDiscountCurve(Currency.USD).get();
    DiscountFactors discountFactors = ZeroRateDiscountFactors.of(Currency.USD, valuationDate, curve);
    IborIndexRates iborIndexRates = DiscountIborIndexRates.of(IborIndices.USD_LIBOR_3M, discountFactors);

    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(Currency.USD);
    IborIndexRatesKey forwardCurveKey = IborIndexRatesKey.of(IborIndices.USD_LIBOR_3M);
    Map<ObservableKey, Double> quotesMap = parRateData.entrySet().stream()
        .collect(toImmutableMap(tp -> tp.getKey().toObservableKey(), tp -> tp.getValue()));
    Map<MarketDataKey<?>, Object> marketDataMap = ImmutableMap.<MarketDataKey<?>, Object>builder()
        .putAll(quotesMap)
        .put(discountFactorsKey, discountFactors)
        .put(forwardCurveKey, iborIndexRates)
        .build();
    MarketDataMap calculationMarketData = new MarketDataMap(valuationDate, marketDataMap, ImmutableMap.of());
    MarketDataRatesProvider ratesProvider =
        new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(calculationMarketData, 0));

    // The PV should be zero for an instrument used to build the curve
    nodes.stream().forEach(node -> checkFraPvIsZero(node, valuationDate, ratesProvider, calculationMarketData));
  }

  public void roundTripFraAndFixedFloatSwap() {
    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    InterpolatedNodalCurveDefinition curveDefn = CurveTestUtils.fraSwapCurveDefinition();
    CurveName curveName = curveDefn.getName();
    List<CurveNode> nodes = curveDefn.getNodes();

    CurveGroupDefinition groupDefn = CurveGroupDefinition.builder()
        .name(groupName)
        .addCurve(curveDefn, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    CurveGroupMarketDataFunction function =
        new CurveGroupMarketDataFunction(RootFinderConfig.defaults(), CalibrationMeasures.DEFAULT);
    LocalDate valuationDate = date(2011, 3, 8);

    Map<ObservableId, Double> parRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(CurveTestUtils.id(nodes.get(0)), 0.0037)
        .put(CurveTestUtils.id(nodes.get(1)), 0.0054)
        .put(CurveTestUtils.id(nodes.get(2)), 0.005)
        .put(CurveTestUtils.id(nodes.get(3)), 0.0087)
        .put(CurveTestUtils.id(nodes.get(4)), 0.012)
        .build();

    ParRates parRates = ParRates.of(parRateData, DefaultCurveMetadata.of(curveName));
    MarketEnvironment marketData = MarketEnvironment.builder(valuationDate)
        .addValue(ParRatesId.of(groupName, curveName, MarketDataFeed.NONE), parRates)
        .build();

    Result<CurveGroup> result = function.buildCurveGroup(groupDefn, marketData, MarketDataFeed.NONE);
    assertThat(result).isSuccess();
    CurveGroup curveGroup = result.getValue();
    Curve curve = curveGroup.findDiscountCurve(Currency.USD).get();
    DiscountFactors discountFactors = ZeroRateDiscountFactors.of(Currency.USD, valuationDate, curve);
    IborIndexRates iborIndexRates = DiscountIborIndexRates.of(IborIndices.USD_LIBOR_3M, discountFactors);

    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(Currency.USD);
    IborIndexRatesKey forwardCurveKey = IborIndexRatesKey.of(IborIndices.USD_LIBOR_3M);
    Map<ObservableKey, Double> quotesMap = parRateData.entrySet().stream()
        .collect(toImmutableMap(tp -> tp.getKey().toObservableKey(), tp -> tp.getValue()));
    Map<MarketDataKey<?>, Object> marketDataMap = ImmutableMap.<MarketDataKey<?>, Object>builder()
        .putAll(quotesMap)
        .put(discountFactorsKey, discountFactors)
        .put(forwardCurveKey, iborIndexRates)
        .build();
    MarketDataMap calculationMarketData = new MarketDataMap(valuationDate, marketDataMap, ImmutableMap.of());
    MarketDataRatesProvider ratesProvider =
        new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(calculationMarketData, 0));

    checkFraPvIsZero((FraCurveNode) nodes.get(0), valuationDate, ratesProvider, calculationMarketData);
    checkFraPvIsZero((FraCurveNode) nodes.get(1), valuationDate, ratesProvider, calculationMarketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(2), valuationDate, ratesProvider, calculationMarketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(3), valuationDate, ratesProvider, calculationMarketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(4), valuationDate, ratesProvider, calculationMarketData);
  }

  /**
   * Tests that par rates are required for curves.
   */
  public void requirements() {
    FraCurveNode node1x4 = CurveTestUtils.fraNode(1, "foo");
    List<CurveNode> nodes = ImmutableList.of(node1x4);
    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA Curve");
    MarketDataFeed feed = MarketDataFeed.of("TestFeed");

    InterpolatedNodalCurveDefinition curveDefn = InterpolatedNodalCurveDefinition.builder()
        .name(curveName)
        .nodes(nodes)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();

    CurveGroupDefinition groupDefn = CurveGroupDefinition.builder()
        .name(groupName)
        .addCurve(curveDefn, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupName, groupDefn)
        .build();

    CurveGroupMarketDataFunction function =
        new CurveGroupMarketDataFunction(RootFinderConfig.defaults(), CalibrationMeasures.DEFAULT);
    CurveGroupId curveGroupId = CurveGroupId.of(groupName, feed);
    MarketDataRequirements requirements = function.requirements(curveGroupId, marketDataConfig);

    assertThat(requirements.getNonObservables()).contains(ParRatesId.of(groupName, curveName, feed));
  }

  public void metadata() {
    CurveGroupName groupName = CurveGroupName.of("Curve Group");

    InterpolatedNodalCurveDefinition fraCurveDefn = CurveTestUtils.fraCurveDefinition();
    List<CurveNode> fraNodes = fraCurveDefn.getNodes();

    CurveGroupDefinition groupDefn = CurveGroupDefinition.builder()
        .name(groupName)
        .addForwardCurve(fraCurveDefn, IborIndices.USD_LIBOR_3M)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupName, groupDefn)
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

    LocalDate valuationDate = date(2011, 3, 8);
    ParRates fraParRates = ParRates.of(fraParRateData, fraCurveDefn.metadata(valuationDate));
    MarketEnvironment marketData = MarketEnvironment.builder(valuationDate)
        .addValue(ParRatesId.of(groupName, fraCurveDefn.getName(), MarketDataFeed.NONE), fraParRates)
        .build();

    CurveGroupMarketDataFunction function =
        new CurveGroupMarketDataFunction(RootFinderConfig.defaults(), CalibrationMeasures.DEFAULT);
    Result<CurveGroup> result = function.build(curveGroupId, marketData, marketDataConfig);

    assertThat(result).isSuccess();
    CurveGroup curveGroup = result.getValue();

    // Check the FRA curve identifiers are the expected tenors
    Curve forwardCurve = curveGroup.findForwardCurve(IborIndices.USD_LIBOR_3M).get();
    List<CurveParameterMetadata> forwardMetadata = forwardCurve.getMetadata().getParameterMetadata().get();

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
      ObservableValues marketDataMap) {

    Trade trade = node.trade(valuationDate, marketDataMap);
    CurrencyAmount currencyAmount = DiscountingFraTradePricer.DEFAULT.presentValue((FraTrade) trade, ratesProvider);
    double pv = currencyAmount.getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

  private void checkSwapPvIsZero(
      FixedIborSwapCurveNode node,
      LocalDate valuationDate,
      RatesProvider ratesProvider,
      ObservableValues marketDataMap) {

    Trade trade = node.trade(valuationDate, marketDataMap);
    MultiCurrencyAmount amount = DiscountingSwapTradePricer.DEFAULT.presentValue((SwapTrade) trade, ratesProvider);
    double pv = amount.getAmount(Currency.USD).getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

}
