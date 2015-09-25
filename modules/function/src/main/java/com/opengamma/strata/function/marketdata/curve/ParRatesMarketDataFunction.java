/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.not;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.function.MarketDataFunction;
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
  public Result<ParRates> build(ParRatesId id, MarketDataLookup marketData, MarketDataConfig marketDataConfig) {
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
    Map<ObservableId, Double> rates = marketData.getObservableValues(requirements);
    CurveMetadata curveMetadata = curveDefn.metadata(marketData.getValuationDate());
    ParRates parRates = ParRates.of(rates, curveMetadata);
    return Result.success(parRates);
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
