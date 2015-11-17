/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static com.opengamma.strata.collect.Guavate.zip;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.id.CurveInputsId;

/**
 * Market data function that builds the input data used when calibrating a curve.
 */
public final class CurveInputsMarketDataFunction implements MarketDataFunction<CurveInputs, CurveInputsId> {

  @Override
  public MarketDataRequirements requirements(CurveInputsId id, MarketDataConfig marketDataConfig) {
    CurveGroupDefinition groupConfig = marketDataConfig.get(CurveGroupDefinition.class, id.getCurveGroupName());
    Optional<CurveGroupEntry> optionalEntry = groupConfig.findEntry(id.getCurveName());

    if (!optionalEntry.isPresent()) {
      return MarketDataRequirements.empty();
    }
    CurveGroupEntry entry = optionalEntry.get();

    if (!(entry.getCurveDefinition() instanceof InterpolatedNodalCurveDefinition)) {
      return MarketDataRequirements.empty();
    }
    InterpolatedNodalCurveDefinition curveDefn = (InterpolatedNodalCurveDefinition) entry.getCurveDefinition();
    MarketDataFeed marketDataFeed = id.getMarketDataFeed();
    Set<? extends SimpleMarketDataKey<?>> requirementKeys = nodeRequirements(curveDefn);
    Set<MarketDataId<?>> requirements = requirementKeys.stream()
        .map(key -> key.toMarketDataId(marketDataFeed))
        .collect(toImmutableSet());
    return MarketDataRequirements.builder().addValues(requirements).build();
  }

  @Override
  public MarketDataBox<CurveInputs> build(
      CurveInputsId id,
      CalculationEnvironment marketData,
      MarketDataConfig marketDataConfig) {

    CurveGroupName groupName = id.getCurveGroupName();
    CurveName curveName = id.getCurveName();
    CurveGroupDefinition groupDefn = marketDataConfig.get(CurveGroupDefinition.class, groupName);
    Optional<CurveGroupEntry> optionalEntry = groupDefn.findEntry(curveName);

    if (!optionalEntry.isPresent()) {
      throw new IllegalArgumentException(Messages.format("No curve named '{}' found in group '{}'", curveName, groupName));
    }
    CurveGroupEntry entry = optionalEntry.get();
    NodalCurveDefinition curveDefn = entry.getCurveDefinition();
    MarketDataFeed marketDataFeed = id.getMarketDataFeed();
    Set<? extends SimpleMarketDataKey<?>> requirements = nodeRequirements(curveDefn);
    Map<? extends MarketDataKey<?>, MarketDataBox<?>> marketDataValues =
        getMarketDataValues(marketData, requirements, marketDataFeed);
    MarketDataBox<LocalDate> valuationDate = marketData.getValuationDate();
    // Do any of the inputs contain values for multiple scenarios, or do they contain 1 value each?
    boolean multipleInputValues = marketDataValues.values().stream().anyMatch(MarketDataBox::isScenarioValue);
    boolean multipleValuationDates = valuationDate.isScenarioValue();

    return multipleInputValues || multipleValuationDates ?
        buildMultipleCurveInputs(curveDefn, marketDataValues, valuationDate) :
        buildSingleCurveInputs(curveDefn, marketDataValues, valuationDate);
  }

  private MarketDataBox<CurveInputs> buildSingleCurveInputs(
      NodalCurveDefinition curveDefn,
      Map<? extends MarketDataKey<?>, MarketDataBox<?>> marketData,
      MarketDataBox<LocalDate> valuationDate) {

    // There is only a single map of values and single valuation date - create a single CurveInputs instance
    CurveMetadata curveMetadata = curveDefn.metadata(valuationDate.getSingleValue());
    Map<? extends MarketDataKey<?>, ?> singleMarketDataValues = marketData.entrySet().stream()
        .collect(toImmutableMap(e -> e.getKey(), e -> e.getValue().getSingleValue()));

    CurveInputs curveInputs = CurveInputs.of(singleMarketDataValues, curveMetadata);
    return MarketDataBox.ofSingleValue(curveInputs);
  }

  private MarketDataBox<CurveInputs> buildMultipleCurveInputs(
      NodalCurveDefinition curveDefn,
      Map<? extends MarketDataKey<?>, MarketDataBox<?>> marketData,
      MarketDataBox<LocalDate> valuationDate) {

    // If there are multiple values for any of the input data values or for the valuation dates then we need to create
    // multiple sets of inputs
    int scenarioCount = scenarioCount(valuationDate, marketData);

    List<CurveMetadata> curveMetadata = IntStream.range(0, scenarioCount)
        .mapToObj(valuationDate::getValue)
        .map(curveDefn::metadata)
        .collect(toImmutableList());

    List<Map<? extends MarketDataKey<?>, ?>> scenarioValues = IntStream.range(0, scenarioCount)
        .mapToObj(i -> buildScenarioValues(marketData, i))
        .collect(toImmutableList());

    List<CurveInputs> curveInputs = zip(scenarioValues.stream(), curveMetadata.stream())
        .map(pair -> CurveInputs.of(pair.getFirst(), pair.getSecond()))
        .collect(toImmutableList());

    return MarketDataBox.ofScenarioValues(curveInputs);
  }

  /**
   * Builds a map of market data key to market data value for a single scenario.
   *
   * @param values  the market data values for all scenarios
   * @param scenarioIndex  the index of the scenario
   * @return map of market data values for one scenario
   */
  private static Map<? extends MarketDataKey<?>, ?> buildScenarioValues(
      Map<? extends MarketDataKey<?>, MarketDataBox<?>> values,
      int scenarioIndex) {

    return values.entrySet().stream().collect(toImmutableMap(e -> e.getKey(), e -> e.getValue().getValue(scenarioIndex)));
  }

  private static int scenarioCount(
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataKey<?>, MarketDataBox<?>> marketData) {

    int scenarioCount = 0;

    if (valuationDate.isScenarioValue()) {
      scenarioCount = valuationDate.getScenarioCount();
    }
    for (Map.Entry<? extends MarketDataKey<?>, MarketDataBox<?>> entry : marketData.entrySet()) {
      MarketDataBox<?> box = entry.getValue();
      MarketDataKey<?> id = entry.getKey();

      if (box.isScenarioValue()) {
        int boxScenarioCount = box.getScenarioCount();

        if (scenarioCount == 0) {
          scenarioCount = boxScenarioCount;
        } else {
          if (scenarioCount != boxScenarioCount) {
            throw new IllegalArgumentException(
                Messages.format(
                    "There are {} scenarios for ID {} which does not match the previous scenario count {}",
                    boxScenarioCount,
                    id,
                    scenarioCount));
          }
        }
      }
    }
    if (scenarioCount != 0) {
      return scenarioCount;
    }
    // This shouldn't happen, this method is only called after checking at least one of the values contains data
    // for multiple scenarios.
    throw new IllegalArgumentException("Cannot count the scenarios, all data contained single values");
  }

  @Override
  public Class<CurveInputsId> getMarketDataIdType() {
    return CurveInputsId.class;
  }

  /**
   * Returns requirements for the market data needed by the curve nodes to build trades.
   *
   * @param curveDefn  the curve definition containing the nodes
   * @return requirements for the market data needed by the nodes to build trades
   */
  private static Set<? extends SimpleMarketDataKey<?>> nodeRequirements(NodalCurveDefinition curveDefn) {
    return curveDefn.getNodes().stream()
        .flatMap(node -> node.requirements().stream())
        .collect(toImmutableSet());
  }

  private static Map<? extends MarketDataKey<?>, MarketDataBox<?>> getMarketDataValues(
      CalculationEnvironment marketData,
      Set<? extends SimpleMarketDataKey<?>> keys,
      MarketDataFeed marketDataFeed) {

    return keys.stream().collect(toImmutableMap(k -> k, k -> marketData.getValue(k.toMarketDataId(marketDataFeed))));
  }
}
