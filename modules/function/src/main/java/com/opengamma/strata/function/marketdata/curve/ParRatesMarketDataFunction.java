/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.not;
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
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.calc.marketdata.MarketDataLookup;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveGroupEntry;
import com.opengamma.strata.market.curve.definition.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.definition.NodalCurveDefinition;
import com.opengamma.strata.market.id.ParRatesId;

/**
 * Market data function that builds the par rates used when calibrating a curve.
 */
public final class ParRatesMarketDataFunction implements MarketDataFunction<ParRates, ParRatesId> {

  @Override
  public MarketDataRequirements requirements(ParRatesId id, MarketDataConfig marketDataConfig) {
    Optional<CurveGroupDefinition> optionalGroup = marketDataConfig.get(CurveGroupDefinition.class, id.getCurveGroupName());

    if (!optionalGroup.isPresent()) {
      return MarketDataRequirements.empty();
    }
    CurveGroupDefinition groupConfig = optionalGroup.get();
    Optional<CurveGroupEntry> optionalEntry = groupConfig.findEntry(id.getCurveName());

    if (!optionalEntry.isPresent()) {
      return MarketDataRequirements.empty();
    }
    CurveGroupEntry entry = optionalEntry.get();

    if (!(entry.getCurveDefinition() instanceof InterpolatedNodalCurveDefinition)) {
      return MarketDataRequirements.empty();
    }
    InterpolatedNodalCurveDefinition curveDefn = (InterpolatedNodalCurveDefinition) entry.getCurveDefinition();
    Set<ObservableId> requirements = nodeRequirements(id.getMarketDataFeed(), curveDefn);
    return MarketDataRequirements.builder().addValues(requirements).build();
  }

  @Override
  public Result<MarketDataBox<ParRates>> build(
      ParRatesId id,
      MarketDataLookup marketData,
      MarketDataConfig marketDataConfig) {

    CurveGroupName groupName = id.getCurveGroupName();
    CurveName curveName = id.getCurveName();
    Optional<CurveGroupDefinition> optionalGroup = marketDataConfig.get(CurveGroupDefinition.class, groupName);

    if (!optionalGroup.isPresent()) {
      return Result.failure(FailureReason.MISSING_DATA, "No configuration found for curve group '{}'", groupName);
    }
    CurveGroupDefinition groupDefn = optionalGroup.get();
    Optional<CurveGroupEntry> optionalEntry = groupDefn.findEntry(curveName);

    if (!optionalEntry.isPresent()) {
      return Result.failure(FailureReason.MISSING_DATA, "No curve named '{}' found in group '{}'", curveName, groupName);
    }
    CurveGroupEntry entry = optionalEntry.get();
    NodalCurveDefinition curveDefn = entry.getCurveDefinition();
    Set<ObservableId> requirements = nodeRequirements(id.getMarketDataFeed(), curveDefn);

    if (!marketData.containsValues(requirements)) {
      Set<ObservableId> missingRequirements = requirements.stream()
          .filter(not(marketData::containsValue))
          .collect(toImmutableSet());
      return Result.failure(FailureReason.MISSING_DATA, "No market data available for '{}'", missingRequirements);
    }
    Map<ObservableId, MarketDataBox<Double>> rates = marketData.getObservableValues(requirements);
    MarketDataBox<LocalDate> valuationDate = marketData.getValuationDate();
    // Do any of the rates contain values for multiple scenarios, or do they contain 1 rate each?
    boolean multipleRatesValues = rates.values().stream().anyMatch(MarketDataBox::isScenarioValue);
    boolean multipleValuationDates = valuationDate.isScenarioValue();

    return multipleRatesValues || multipleValuationDates ?
        buildMultipleParRates(curveDefn, rates, valuationDate) :
        buildSingleParRates(curveDefn, rates, valuationDate);
  }

  private Result<MarketDataBox<ParRates>> buildSingleParRates(
      NodalCurveDefinition curveDefn,
      Map<ObservableId, MarketDataBox<Double>> rates, MarketDataBox<LocalDate> valuationDate) {
    // There is only a single map of rates and single valuation date - create a single ParRates instance
    CurveMetadata curveMetadata = curveDefn.metadata(valuationDate.getSingleValue());
    Map<ObservableId, Double> singleRates = rates.entrySet().stream()
        .collect(toImmutableMap(e -> e.getKey(), e -> e.getValue().getSingleValue()));

    ParRates parRates = ParRates.of(singleRates, curveMetadata);
    return Result.success(MarketDataBox.ofSingleValue(parRates));
  }

  private Result<MarketDataBox<ParRates>> buildMultipleParRates(
      NodalCurveDefinition curveDefn,
      Map<ObservableId, MarketDataBox<Double>> rates, MarketDataBox<LocalDate> valuationDate) {
    // If there are multiple values for any of the rates or for the valuation dates then we need to create
    // multiple sets of ParRates
    Result<Integer> scenarioCountResult = scenarioCount(valuationDate, rates);

    if (scenarioCountResult.isFailure()) {
      return Result.failure(scenarioCountResult);
    }
    int scenarioCount = scenarioCountResult.getValue();

    List<CurveMetadata> curveMetadata = IntStream.range(0, scenarioCount)
        .mapToObj(valuationDate::getValue)
        .map(curveDefn::metadata)
        .collect(toImmutableList());

    List<Map<ObservableId, Double>> singleRates = IntStream.range(0, scenarioCount)
        .mapToObj(i -> buildSingleRates(rates, i))
        .collect(toImmutableList());

    List<ParRates> parRates = zip(singleRates.stream(), curveMetadata.stream())
        .map(pair -> ParRates.of(pair.getFirst(), pair.getSecond()))
        .collect(toImmutableList());

    return Result.success(MarketDataBox.ofScenarioValues(parRates));
  }

  private static Map<ObservableId, Double> buildSingleRates(
      Map<ObservableId, MarketDataBox<Double>> rates,
      int scenarioIndex) {

    return rates.entrySet().stream().collect(toImmutableMap(e -> e.getKey(), e -> e.getValue().getValue(scenarioIndex)));
  }

  private static Result<Integer> scenarioCount(
      MarketDataBox<LocalDate> valuationDate,
      Map<ObservableId, MarketDataBox<Double>> rates) {

    Integer scenarioCount = null;

    if (valuationDate.isScenarioValue()) {
      scenarioCount = valuationDate.getScenarioCount();
    }
    for (Map.Entry<ObservableId, MarketDataBox<Double>> entry : rates.entrySet()) {
      MarketDataBox<Double> box = entry.getValue();
      ObservableId id = entry.getKey();

      if (box.isScenarioValue()) {
        int boxScenarioCount = box.getScenarioCount();

        if (scenarioCount == null) {
          scenarioCount = boxScenarioCount;
        } else {
          if (scenarioCount != boxScenarioCount) {
            return Result.failure(
                FailureReason.INVALID_INPUT,
                "There are {} scenarios for ID {} which does not match the previous scenario count {}",
                boxScenarioCount,
                id,
                scenarioCount);
          }
        }
      }
    }
    if (scenarioCount != null) {
      return Result.success(scenarioCount);
    }
    // This shouldn't happen, this method is only called after checking at least one of the values contains data
    // for multiple scenarios.
    return Result.failure(FailureReason.INVALID_INPUT, "Cannot count the scenarios, all data contained single values");
  }

  @Override
  public Class<ParRatesId> getMarketDataIdType() {
    return ParRatesId.class;
  }

  /**
   * Returns requirements for the market data needed by the curve nodes to build trades.
   *
   * @param feed  the market data feed which provides quotes used to build the curve
   * @param curveDefn  the curve definition containing the nodes
   * @return requirements for the market data needed by the nodes to build trades
   */
  private static Set<ObservableId> nodeRequirements(MarketDataFeed feed, NodalCurveDefinition curveDefn) {
    return curveDefn.getNodes().stream()
        .flatMap(node -> node.requirements().stream())
        .map(key -> key.toObservableId(feed))
        .collect(toImmutableSet());
  }

}
