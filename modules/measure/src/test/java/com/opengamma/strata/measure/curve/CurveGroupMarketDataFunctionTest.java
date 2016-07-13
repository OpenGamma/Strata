/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupId;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.CurveInputsId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.curve.node.FxSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.fra.ResolvedFraTrade;
import com.opengamma.strata.product.fx.type.FxSwapConventions;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Test {@link CurveGroupMarketDataFunction}.
 */
@Test
public class CurveGroupMarketDataFunctionTest {

  /** The calibrator. */
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.standard();
  /** The maximum allowable PV when round-tripping an instrument used to calibrate a curve. */
  private static final double PV_TOLERANCE = 5e-10;
  /** The reference data. */
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /**
   * Tests calibration a curve containing FRAs and pricing the curve instruments using the curve.
   */
  public void roundTripFra() {
    InterpolatedNodalCurveDefinition curveDefn = CurveTestUtils.fraCurveDefinition();

    List<FraCurveNode> nodes = curveDefn.getNodes().stream()
        .map(FraCurveNode.class::cast)
        .collect(toImmutableList());

    List<MarketDataId<?>> keys = nodes.stream().map(CurveTestUtils::key).collect(toImmutableList());
    Map<MarketDataId<?>, Double> inputData = ImmutableMap.<MarketDataId<?>, Double>builder()
        .put(keys.get(0), 0.003)
        .put(keys.get(1), 0.0033)
        .put(keys.get(2), 0.0037)
        .put(keys.get(3), 0.0054)
        .put(keys.get(4), 0.007)
        .put(keys.get(5), 0.0091)
        .put(keys.get(6), 0.0134)
        .build();

    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA Curve");
    CurveInputs curveInputs = CurveInputs.of(inputData, DefaultCurveMetadata.of(curveName));

    CurveGroupDefinition groupDefn = CurveGroupDefinition.builder()
        .name(groupName)
        .addCurve(curveDefn, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction();
    LocalDate valuationDate = date(2011, 3, 8);
    ScenarioMarketData inputMarketData = ImmutableScenarioMarketData.builder(valuationDate)
        .addValue(CurveInputsId.of(groupName, curveName, ObservableSource.NONE), curveInputs)
        .build();
    MarketDataBox<CurveGroup> curveGroup =
        function.buildCurveGroup(groupDefn, CALIBRATOR, inputMarketData, REF_DATA, ObservableSource.NONE);

    Curve curve = curveGroup.getSingleValue().findDiscountCurve(Currency.USD).get();

    Map<MarketDataId<?>, Object> marketDataMap = ImmutableMap.<MarketDataId<?>, Object>builder()
        .putAll(inputData)
        .put(CurveId.of(groupName, curveName), curve)
        .build();

    MarketData marketData = ImmutableMarketData.of(valuationDate, marketDataMap);
    TestMarketDataMap scenarioMarketData = new TestMarketDataMap(valuationDate, marketDataMap, ImmutableMap.of());
    RatesMarketDataLookup lookup = RatesMarketDataLookup.of(groupDefn);
    RatesProvider ratesProvider = lookup.ratesProvider(scenarioMarketData.scenario(0));

    // The PV should be zero for an instrument used to build the curve
    nodes.stream().forEach(node -> checkFraPvIsZero(node, ratesProvider, marketData));
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

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction();
    LocalDate valuationDate = date(2011, 3, 8);

    Map<MarketDataId<?>, Double> inputData = ImmutableMap.<MarketDataId<?>, Double>builder()
        .put(CurveTestUtils.key(nodes.get(0)), 0.0037)
        .put(CurveTestUtils.key(nodes.get(1)), 0.0054)
        .put(CurveTestUtils.key(nodes.get(2)), 0.005)
        .put(CurveTestUtils.key(nodes.get(3)), 0.0087)
        .put(CurveTestUtils.key(nodes.get(4)), 0.012)
        .build();

    CurveInputs curveInputs = CurveInputs.of(inputData, DefaultCurveMetadata.of(curveName));
    ScenarioMarketData inputMarketData = ImmutableScenarioMarketData.builder(valuationDate)
        .addValue(CurveInputsId.of(groupName, curveName, ObservableSource.NONE), curveInputs)
        .build();

    MarketDataBox<CurveGroup> curveGroup =
        function.buildCurveGroup(groupDefn, CALIBRATOR, inputMarketData, REF_DATA, ObservableSource.NONE);
    Curve curve = curveGroup.getSingleValue().findDiscountCurve(Currency.USD).get();

    Map<MarketDataId<?>, Object> marketDataMap = ImmutableMap.<MarketDataId<?>, Object>builder()
        .putAll(inputData)
        .put(CurveId.of(groupName, curveName), curve)
        .build();
    MarketData marketData = ImmutableMarketData.of(valuationDate, marketDataMap);
    TestMarketDataMap scenarioMarketData = new TestMarketDataMap(valuationDate, marketDataMap, ImmutableMap.of());
    RatesMarketDataLookup lookup = RatesMarketDataLookup.of(groupDefn);
    RatesProvider ratesProvider = lookup.ratesProvider(scenarioMarketData.scenario(0));

    checkFraPvIsZero((FraCurveNode) nodes.get(0), ratesProvider, marketData);
    checkFraPvIsZero((FraCurveNode) nodes.get(1), ratesProvider, marketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(2), ratesProvider, marketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(3), ratesProvider, marketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(4), ratesProvider, marketData);
  }

  /**
   * Tests that par rates are required for curves.
   */
  public void requirements() {
    FraCurveNode node1x4 = CurveTestUtils.fraNode(1, "foo");
    FraCurveNode node2x5 = CurveTestUtils.fraNode(2, "foo");
    List<CurveNode> nodes = ImmutableList.of(node1x4, node2x5);
    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA Curve");
    ObservableSource obsSource = ObservableSource.of("Vendor");

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

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction();
    CurveGroupId curveGroupId = CurveGroupId.of(groupName, obsSource);
    MarketDataRequirements requirements = function.requirements(curveGroupId, marketDataConfig);

    assertThat(requirements.getNonObservables()).contains(CurveInputsId.of(groupName, curveName, obsSource));
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

    Map<MarketDataId<?>, Double> fraInputData = ImmutableMap.<MarketDataId<?>, Double>builder()
        .put(CurveTestUtils.key(fraNodes.get(0)), 0.003)
        .put(CurveTestUtils.key(fraNodes.get(1)), 0.0033)
        .put(CurveTestUtils.key(fraNodes.get(2)), 0.0037)
        .put(CurveTestUtils.key(fraNodes.get(3)), 0.0054)
        .put(CurveTestUtils.key(fraNodes.get(4)), 0.007)
        .put(CurveTestUtils.key(fraNodes.get(5)), 0.0091)
        .put(CurveTestUtils.key(fraNodes.get(6)), 0.0134).build();

    LocalDate valuationDate = date(2011, 3, 8);
    CurveInputs fraCurveInputs = CurveInputs.of(fraInputData, fraCurveDefn.metadata(valuationDate, REF_DATA));
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(valuationDate)
        .addValue(CurveInputsId.of(groupName, fraCurveDefn.getName(), ObservableSource.NONE), fraCurveInputs)
        .build();

    CurveGroupMarketDataFunction function = new CurveGroupMarketDataFunction();
    MarketDataBox<CurveGroup> curveGroup = function.build(curveGroupId, marketDataConfig, marketData, REF_DATA);

    // Check the FRA curve identifiers are the expected tenors
    Curve forwardCurve = curveGroup.getSingleValue().findForwardCurve(IborIndices.USD_LIBOR_3M).get();
    List<ParameterMetadata> forwardMetadata = forwardCurve.getMetadata().getParameterMetadata().get();

    List<Object> forwardTenors = forwardMetadata.stream()
        .map(ParameterMetadata::getIdentifier)
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

    List<ParameterMetadata> expectedForwardMetadata = fraNodes.stream()
        .map(node -> node.metadata(valuationDate, REF_DATA))
        .collect(toImmutableList());

    assertThat(forwardMetadata).isEqualTo(expectedForwardMetadata);
  }

  //-------------------------------------------------------------------------
  public void duplicateInputDataKeys() {
    FxSwapTemplate template1 = FxSwapTemplate.of(Period.ofMonths(1), FxSwapConventions.EUR_USD);
    FxSwapTemplate template2 = FxSwapTemplate.of(Period.ofMonths(2), FxSwapConventions.EUR_USD);
    QuoteId pointsKey1a = QuoteId.of(StandardId.of("test", "1a"));
    QuoteId pointsKey1b = QuoteId.of(StandardId.of("test", "1b"));
    QuoteId pointsKey2a = QuoteId.of(StandardId.of("test", "2a"));
    QuoteId pointsKey2b = QuoteId.of(StandardId.of("test", "2b"));
    FxSwapCurveNode node1a = FxSwapCurveNode.of(template1, pointsKey1a);
    FxSwapCurveNode node1b = FxSwapCurveNode.of(template2, pointsKey1b);
    FxSwapCurveNode node2 = FxSwapCurveNode.of(template1, pointsKey2a);
    FxSwapCurveNode node2b = FxSwapCurveNode.of(template2, pointsKey2b);
    CurveName curveName1 = CurveName.of("curve1");
    InterpolatedNodalCurveDefinition curve1 = InterpolatedNodalCurveDefinition.builder()
        .name(curveName1)
        .nodes(node1a, node1b)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_360)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.LINEAR)
        .extrapolatorRight(CurveExtrapolators.LINEAR)
        .build();
    CurveName curveName2 = CurveName.of("curve2");
    InterpolatedNodalCurveDefinition curve2 = InterpolatedNodalCurveDefinition.builder()
        .name(curveName2)
        .nodes(node2, node2b)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_360)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.LINEAR)
        .extrapolatorRight(CurveExtrapolators.LINEAR)
        .build();
    CurveGroupName curveGroupName = CurveGroupName.of("group");
    CurveGroupDefinition groupDefinition = CurveGroupDefinition.builder()
        .name(curveGroupName)
        .addDiscountCurve(curve1, Currency.EUR)
        .addDiscountCurve(curve2, Currency.USD)
        .build();

    CurveGroupMarketDataFunction fn = new CurveGroupMarketDataFunction();
    Map<MarketDataId<?>, Object> marketDataMap1 = ImmutableMap.of(
        FxRateId.of(Currency.EUR, Currency.USD),
        FxRate.of(Currency.EUR, Currency.USD, 1.01),
        pointsKey1a, 0.1d,
        pointsKey1b, 0.2d);
    Map<MarketDataId<?>, Object> marketDataMap2 = ImmutableMap.of(
        FxRateId.of(Currency.EUR, Currency.USD),
        FxRate.of(Currency.EUR, Currency.USD, 1.01),
        pointsKey2a, 0.1d,
        pointsKey2b, 0.2d);
    CurveInputs curveInputs1 = CurveInputs.of(marketDataMap1, DefaultCurveMetadata.of("curve1"));
    CurveInputs curveInputs2 = CurveInputs.of(marketDataMap2, DefaultCurveMetadata.of("curve2"));
    ImmutableScenarioMarketData marketData = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addValue(CurveInputsId.of(curveGroupName, curveName1, ObservableSource.NONE), curveInputs1)
        .addValue(CurveInputsId.of(curveGroupName, curveName2, ObservableSource.NONE), curveInputs2)
        .build();
    fn.buildCurveGroup(groupDefinition, CALIBRATOR, marketData, REF_DATA, ObservableSource.NONE);

    // This has a duplicate key with a different value which should fail
    Map<MarketDataId<?>, Object> badMarketDataMap = ImmutableMap.of(
        FxRateId.of(Currency.EUR, Currency.USD), FxRate.of(Currency.EUR, Currency.USD, 1.02),
        pointsKey2a, 0.2d);
    CurveInputs badCurveInputs = CurveInputs.of(badMarketDataMap, DefaultCurveMetadata.of("curve2"));
    ScenarioMarketData badMarketData = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addValue(CurveInputsId.of(curveGroupName, curveName1, ObservableSource.NONE), curveInputs1)
        .addValue(CurveInputsId.of(curveGroupName, curveName2, ObservableSource.NONE), badCurveInputs)
        .build();
    String msg = "Multiple unequal values found for identifier .*\\. Values: .* and .*";
    assertThrowsIllegalArg(
        () -> fn.buildCurveGroup(groupDefinition, CALIBRATOR, badMarketData, REF_DATA, ObservableSource.NONE), msg);
  }

  //-----------------------------------------------------------------------------------------------------------

  private void checkFraPvIsZero(
      FraCurveNode node,
      RatesProvider ratesProvider,
      MarketData marketDataMap) {

    ResolvedFraTrade trade = node.resolvedTrade(1d, marketDataMap, REF_DATA);
    CurrencyAmount currencyAmount = DiscountingFraTradePricer.DEFAULT.presentValue(trade, ratesProvider);
    double pv = currencyAmount.getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

  private void checkSwapPvIsZero(
      FixedIborSwapCurveNode node,
      RatesProvider ratesProvider,
      MarketData marketDataMap) {

    ResolvedSwapTrade trade = node.resolvedTrade(1d, marketDataMap, REF_DATA);
    MultiCurrencyAmount amount = DiscountingSwapTradePricer.DEFAULT.presentValue(trade, ratesProvider);
    double pv = amount.getAmount(Currency.USD).getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

}
