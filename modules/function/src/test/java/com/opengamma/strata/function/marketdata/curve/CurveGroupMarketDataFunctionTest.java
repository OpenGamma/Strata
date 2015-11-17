/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.CurveInputsId;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IborIndexRatesKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.DiscountIborIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.swap.SwapTrade;

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

    List<MarketDataKey<?>> keys = nodes.stream().map(CurveTestUtils::key).collect(toImmutableList());
    Map<MarketDataKey<?>, Double> inputData = ImmutableMap.<MarketDataKey<?>, Double>builder()
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

    CurveGroupMarketDataFunction function =
        new CurveGroupMarketDataFunction(RootFinderConfig.defaults(), CalibrationMeasures.DEFAULT);
    LocalDate valuationDate = date(2011, 3, 8);
    MarketEnvironment marketEnvironment = MarketEnvironment.builder()
        .valuationDate(valuationDate)
        .addValue(CurveInputsId.of(groupName, curveName, MarketDataFeed.NONE), curveInputs)
        .build();
    MarketDataBox<CurveGroup> curveGroup = function.buildCurveGroup(groupDefn, marketEnvironment, MarketDataFeed.NONE);

    Curve curve = curveGroup.getSingleValue().findDiscountCurve(Currency.USD).get();
    DiscountFactors discountFactors = ZeroRateDiscountFactors.of(Currency.USD, valuationDate, curve);
    IborIndexRates iborIndexRates = DiscountIborIndexRates.of(IborIndices.USD_LIBOR_3M, discountFactors);

    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(Currency.USD);
    IborIndexRatesKey forwardCurveKey = IborIndexRatesKey.of(IborIndices.USD_LIBOR_3M);
    Map<MarketDataKey<?>, Object> marketDataMap = ImmutableMap.<MarketDataKey<?>, Object>builder()
        .putAll(inputData)
        .put(discountFactorsKey, discountFactors)
        .put(forwardCurveKey, iborIndexRates)
        .build();

    MarketData marketData = MarketData.of(marketDataMap);
    TestMarketDataMap calculationMarketData = new TestMarketDataMap(valuationDate, marketDataMap, ImmutableMap.of());
    MarketDataRatesProvider ratesProvider =
        new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(calculationMarketData, 0));

    // The PV should be zero for an instrument used to build the curve
    nodes.stream().forEach(node -> checkFraPvIsZero(node, valuationDate, ratesProvider, marketData));
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

    Map<MarketDataKey<?>, Double> inputData = ImmutableMap.<MarketDataKey<?>, Double>builder()
        .put(CurveTestUtils.key(nodes.get(0)), 0.0037)
        .put(CurveTestUtils.key(nodes.get(1)), 0.0054)
        .put(CurveTestUtils.key(nodes.get(2)), 0.005)
        .put(CurveTestUtils.key(nodes.get(3)), 0.0087)
        .put(CurveTestUtils.key(nodes.get(4)), 0.012)
        .build();

    CurveInputs curveInputs = CurveInputs.of(inputData, DefaultCurveMetadata.of(curveName));
    MarketEnvironment marketEnvironment = MarketEnvironment.builder()
        .valuationDate(valuationDate)
        .addValue(CurveInputsId.of(groupName, curveName, MarketDataFeed.NONE), curveInputs)
        .build();

    MarketDataBox<CurveGroup> curveGroup = function.buildCurveGroup(groupDefn, marketEnvironment, MarketDataFeed.NONE);
    Curve curve = curveGroup.getSingleValue().findDiscountCurve(Currency.USD).get();
    DiscountFactors discountFactors = ZeroRateDiscountFactors.of(Currency.USD, valuationDate, curve);
    IborIndexRates iborIndexRates = DiscountIborIndexRates.of(IborIndices.USD_LIBOR_3M, discountFactors);

    DiscountFactorsKey discountFactorsKey = DiscountFactorsKey.of(Currency.USD);
    IborIndexRatesKey forwardCurveKey = IborIndexRatesKey.of(IborIndices.USD_LIBOR_3M);
    Map<MarketDataKey<?>, Object> marketDataMap = ImmutableMap.<MarketDataKey<?>, Object>builder()
        .putAll(inputData)
        .put(discountFactorsKey, discountFactors)
        .put(forwardCurveKey, iborIndexRates)
        .build();
    MarketData marketData = MarketData.of(marketDataMap);
    TestMarketDataMap calculationMarketData = new TestMarketDataMap(valuationDate, marketDataMap, ImmutableMap.of());
    MarketDataRatesProvider ratesProvider =
        new MarketDataRatesProvider(new DefaultSingleCalculationMarketData(calculationMarketData, 0));

    checkFraPvIsZero((FraCurveNode) nodes.get(0), valuationDate, ratesProvider, marketData);
    checkFraPvIsZero((FraCurveNode) nodes.get(1), valuationDate, ratesProvider, marketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(2), valuationDate, ratesProvider, marketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(3), valuationDate, ratesProvider, marketData);
    checkSwapPvIsZero((FixedIborSwapCurveNode) nodes.get(4), valuationDate, ratesProvider, marketData);
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

    assertThat(requirements.getNonObservables()).contains(CurveInputsId.of(groupName, curveName, feed));
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

    Map<MarketDataKey<?>, Double> fraInputData = ImmutableMap.<MarketDataKey<?>, Double>builder()
        .put(CurveTestUtils.key(fraNodes.get(0)), 0.003)
        .put(CurveTestUtils.key(fraNodes.get(1)), 0.0033)
        .put(CurveTestUtils.key(fraNodes.get(2)), 0.0037)
        .put(CurveTestUtils.key(fraNodes.get(3)), 0.0054)
        .put(CurveTestUtils.key(fraNodes.get(4)), 0.007)
        .put(CurveTestUtils.key(fraNodes.get(5)), 0.0091)
        .put(CurveTestUtils.key(fraNodes.get(6)), 0.0134).build();

    LocalDate valuationDate = date(2011, 3, 8);
    CurveInputs fraCurveInputs = CurveInputs.of(fraInputData, fraCurveDefn.metadata(valuationDate));
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(valuationDate)
        .addValue(CurveInputsId.of(groupName, fraCurveDefn.getName(), MarketDataFeed.NONE), fraCurveInputs)
        .build();

    CurveGroupMarketDataFunction function =
        new CurveGroupMarketDataFunction(RootFinderConfig.defaults(), CalibrationMeasures.DEFAULT);
    MarketDataBox<CurveGroup> curveGroup = function.build(curveGroupId, marketData, marketDataConfig);

    // Check the FRA curve identifiers are the expected tenors
    Curve forwardCurve = curveGroup.getSingleValue().findForwardCurve(IborIndices.USD_LIBOR_3M).get();
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
      MarketData marketDataMap) {

    Trade trade = node.trade(valuationDate, marketDataMap);
    CurrencyAmount currencyAmount = DiscountingFraTradePricer.DEFAULT.presentValue((FraTrade) trade, ratesProvider);
    double pv = currencyAmount.getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

  private void checkSwapPvIsZero(
      FixedIborSwapCurveNode node,
      LocalDate valuationDate,
      RatesProvider ratesProvider,
      MarketData marketDataMap) {

    Trade trade = node.trade(valuationDate, marketDataMap);
    MultiCurrencyAmount amount = DiscountingSwapTradePricer.DEFAULT.presentValue((SwapTrade) trade, ratesProvider);
    double pv = amount.getAmount(Currency.USD).getAmount();
    assertThat(pv).isCloseTo(0, offset(PV_TOLERANCE));
  }

}
