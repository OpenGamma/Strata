/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.CurveInputsId;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurveDefinition;

/**
 * Market data function that builds the input data used when calibrating a curve.
 */
public final class CurveInputsMarketDataFunction implements MarketDataFunction<CurveInputs, CurveInputsId> {

  @Override
  public MarketDataRequirements requirements(CurveInputsId id, MarketDataConfig marketDataConfig) {
    CurveGroupDefinition groupConfig = marketDataConfig.get(CurveGroupDefinition.class, id.getCurveGroupName());
    Optional<NodalCurveDefinition> optionalDefinition = groupConfig.findCurveDefinition(id.getCurveName());
    if (!optionalDefinition.isPresent()) {
      return MarketDataRequirements.empty();
    }
    NodalCurveDefinition definition = optionalDefinition.get();
    return MarketDataRequirements.builder().addValues(nodeRequirements(ImmutableList.of(definition))).build();
  }

  @Override
  public MarketDataBox<CurveInputs> build(
      CurveInputsId id,
      MarketDataConfig marketDataConfig,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    CurveGroupName groupName = id.getCurveGroupName();
    CurveName curveName = id.getCurveName();
    CurveGroupDefinition groupDefn = marketDataConfig.get(CurveGroupDefinition.class, groupName);
    Optional<NodalCurveDefinition> optionalDefinition = groupDefn.findCurveDefinition(id.getCurveName());

    if (!optionalDefinition.isPresent()) {
      throw new IllegalArgumentException(Messages.format("No curve named '{}' found in group '{}'", curveName, groupName));
    }
    NodalCurveDefinition configuredDefn = optionalDefinition.get();
    // determine market data needs
    MarketDataBox<LocalDate> valuationDates = marketData.getValuationDate();
    boolean multipleValuationDates = valuationDates.isScenarioValue();
    // curve definition can vary for each valuation date
    if (multipleValuationDates) {
      List<NodalCurveDefinition> curveDefns = IntStream.range(0, valuationDates.getScenarioCount())
          .mapToObj(valuationDates::getValue)
          .map((LocalDate valDate) -> configuredDefn.filtered(valDate, refData))
          .collect(toImmutableList());

      Set<? extends MarketDataId<?>> requirements = nodeRequirements(curveDefns);
      ObservableSource obsSource = id.getObservableSource();
      Map<? extends MarketDataId<?>, MarketDataBox<?>> marketDataValues =
          getMarketDataValues(marketData, requirements, obsSource);
      return buildMultipleCurveInputs(MarketDataBox.ofScenarioValues(curveDefns), marketDataValues, valuationDates, refData);
    }
    // only one valuation date
    LocalDate valuationDate = valuationDates.getValue(0);
    NodalCurveDefinition filteredDefn = configuredDefn.filtered(valuationDate, refData);
    Set<? extends MarketDataId<?>> requirements = nodeRequirements(ImmutableList.of(filteredDefn));
    ObservableSource obsSource = id.getObservableSource();
    Map<? extends MarketDataId<?>, MarketDataBox<?>> marketDataValues =
        getMarketDataValues(marketData, requirements, obsSource);
    // Do any of the inputs contain values for multiple scenarios, or do they contain 1 value each?
    boolean multipleInputValues = marketDataValues.values().stream().anyMatch(MarketDataBox::isScenarioValue);

    return multipleInputValues || multipleValuationDates ?
        buildMultipleCurveInputs(MarketDataBox.ofSingleValue(filteredDefn), marketDataValues, valuationDates, refData) :
        buildSingleCurveInputs(filteredDefn, marketDataValues, valuationDate, refData);
  }

  // one valuation date, one set of market data
  private MarketDataBox<CurveInputs> buildSingleCurveInputs(
      NodalCurveDefinition filteredDefn,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> marketData,
      LocalDate valuationDate,
      ReferenceData refData) {

    // There is only a single map of values and single valuation date - create a single CurveInputs instance
    CurveMetadata curveMetadata = filteredDefn.metadata(valuationDate, refData);
    Map<? extends MarketDataId<?>, ?> singleMarketDataValues = MapStream.of(marketData)
        .mapValues(box -> box.getSingleValue())
        .toMap();

    CurveInputs curveInputs = CurveInputs.of(singleMarketDataValues, curveMetadata);
    return MarketDataBox.ofSingleValue(curveInputs);
  }

  // one valuation date, scenario market data
  private MarketDataBox<CurveInputs> buildMultipleCurveInputs(
      MarketDataBox<NodalCurveDefinition> filteredDefns,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> marketData,
      MarketDataBox<LocalDate> valuationDates,
      ReferenceData refData) {

    // If there are multiple values for any of the input data values or for the valuation
    // dates then we need to create multiple sets of inputs
    int scenarioCount = scenarioCount(valuationDates, marketData);

    ImmutableList.Builder<CurveMetadata> curveMetadataBuilder = ImmutableList.builder();
    for (int i = 0; i < scenarioCount; i++) {
      LocalDate valDate = valuationDates.getValue(i);
      NodalCurveDefinition defn = filteredDefns.getValue(i);
      curveMetadataBuilder.add(defn.metadata(valDate, refData));
    }
    List<CurveMetadata> curveMetadata = curveMetadataBuilder.build();

    List<Map<? extends MarketDataId<?>, ?>> scenarioValues = IntStream.range(0, scenarioCount)
        .mapToObj(i -> buildScenarioValues(marketData, i))
        .collect(toImmutableList());

    List<CurveInputs> curveInputs = zip(scenarioValues.stream(), curveMetadata.stream())
        .map(pair -> CurveInputs.of(pair.getFirst(), pair.getSecond()))
        .collect(toImmutableList());

    return MarketDataBox.ofScenarioValues(curveInputs);
  }

  /**
   * Builds a map of market data identifier to market data value for a single scenario.
   *
   * @param values  the market data values for all scenarios
   * @param scenarioIndex  the index of the scenario
   * @return map of market data values for one scenario
   */
  private static Map<? extends MarketDataId<?>, ?> buildScenarioValues(
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      int scenarioIndex) {

    return MapStream.of(values).mapValues(box -> box.getValue(scenarioIndex)).toMap();
  }

  private static int scenarioCount(
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> marketData) {

    int scenarioCount = 0;

    if (valuationDate.isScenarioValue()) {
      scenarioCount = valuationDate.getScenarioCount();
    }
    for (Map.Entry<? extends MarketDataId<?>, MarketDataBox<?>> entry : marketData.entrySet()) {
      MarketDataBox<?> box = entry.getValue();
      MarketDataId<?> id = entry.getKey();

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
  private static Set<? extends MarketDataId<?>> nodeRequirements(List<NodalCurveDefinition> curveDefns) {
    return curveDefns.stream()
        .flatMap(defn -> defn.getNodes().stream())
        .flatMap(node -> node.requirements().stream())
        .collect(toImmutableSet());
  }

  private static Map<? extends MarketDataId<?>, MarketDataBox<?>> getMarketDataValues(
      ScenarioMarketData marketData,
      Set<? extends MarketDataId<?>> ids,
      ObservableSource observableSource) {

    return ids.stream().collect(toImmutableMap(k -> k, k -> marketData.getValue(k)));
  }
}
