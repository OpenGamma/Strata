/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Builds a dependency tree for the items of market used in a set of calculations.
 * <p>
 * The root of the tree represents the calculations and the child nodes represent items of market data on which
 * the calculations depend. Market data can depend on other market data, creating a tree structure of unlimited
 * depth.
 * <p>
 * Edges between nodes represent dependencies on items of market data. Leaf nodes represent market data
 * with no unsatisfied dependencies which can be built immediately.
 * <p>
 * See {@link MarketDataNode} for more detailed documentation.
 *
 * @see MarketDataNode
 */
final class DependencyTreeBuilder {

  /** The market data supplied by the user. */
  private final ScenarioMarketData suppliedData;

  /** The functions that create items of market data. */
  private final Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions;

  /** The requirements for market data used in a set of calculations. */
  private final MarketDataRequirements requirements;

  /** Configuration specifying how market data values should be built. */
  private final MarketDataConfig marketDataConfig;

  /**
   * Returns a tree builder that builds the dependency tree for the market data required by a set of calculations.
   *
   * @param suppliedData  market data supplied by the user
   * @param requirements  specifies the market data required for the calculations
   * @param marketDataConfig  configuration specifying how market data values should be built
   * @param functions  functions that create items of market data
   * @return a tree builder that builds the dependency tree for the market data required by a set of calculations
   */
  static DependencyTreeBuilder of(
      ScenarioMarketData suppliedData,
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions) {

    return new DependencyTreeBuilder(suppliedData, requirements, marketDataConfig, functions);
  }

  private DependencyTreeBuilder(
      ScenarioMarketData suppliedData,
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions) {

    this.suppliedData = suppliedData;
    this.requirements = requirements;
    this.marketDataConfig = marketDataConfig;
    this.functions = functions;
  }

  /**
   * Returns nodes representing the dependencies of the market data required for a set of calculations.
   *
   * @return nodes representing the dependencies of the market data required for a set of calculations
   */
  List<MarketDataNode> dependencyNodes() {
    return dependencyNodes(requirements);
  }

  /**
   * Returns nodes representing the dependencies of a set of market data.
   *
   * @param requirements  requirements for market data needed for a set of calculations
   * @return nodes representing the dependencies of a set of market data
   */
  private List<MarketDataNode> dependencyNodes(MarketDataRequirements requirements) {

    List<MarketDataNode> observableNodes =
        buildNodes(requirements.getObservables(), MarketDataNode.DataType.SINGLE_VALUE);

    List<MarketDataNode> nonObservableNodes =
        buildNodes(requirements.getNonObservables(), MarketDataNode.DataType.SINGLE_VALUE);

    List<MarketDataNode> timeSeriesNodes =
        buildNodes(requirements.getTimeSeries(), MarketDataNode.DataType.TIME_SERIES);

    return ImmutableList.<MarketDataNode>builder()
        .addAll(observableNodes)
        .addAll(nonObservableNodes)
        .addAll(timeSeriesNodes)
        .build();
  }

  /**
   * Builds nodes for a set of market data IDs.
   *
   * @param ids  the IDs
   * @param dataType  the type of data represented by the IDs, either single values or time series of values
   * @return market data nodes for the IDs
   */
  private List<MarketDataNode> buildNodes(Set<? extends MarketDataId<?>> ids, MarketDataNode.DataType dataType) {
    return ids.stream()
        .map(id -> buildNode(id, dataType))
        .collect(toImmutableList());
  }

  /**
   * Builds a node for a market data ID.
   *
   * @param id  the ID
   * @param dataType  the type of data represented by the ID, either a single value or a time series of values
   * @return a market data node for the ID
   */
  private MarketDataNode buildNode(MarketDataId<?> id, MarketDataNode.DataType dataType) {

    // Observable data has special handling and is guaranteed to have a function.
    // Supplied data definitely has no dependencies because it already exists and doesn't need to be built.
    if (id instanceof ObservableId || isSupplied(id, dataType, suppliedData)) {
      return MarketDataNode.leaf(id, dataType);
    }
    // Find the function that can build the data identified by the ID
    @SuppressWarnings("rawtypes")
    MarketDataFunction function = functions.get(id.getClass());

    if (function != null) {
      try {
        @SuppressWarnings("unchecked")
        MarketDataRequirements requirements = function.requirements(id, marketDataConfig);
        return MarketDataNode.child(id, dataType, dependencyNodes(requirements));
      } catch (Exception e) {
        return MarketDataNode.child(id, dataType, ImmutableList.of());
      }
    } else {
      // If there is no function insert a leaf node. It will be flagged as an error when the data is built
      return MarketDataNode.leaf(id, dataType);
    }
  }

  /**
   * Returns true if the market data identified by the ID and data type is present in the supplied data.
   *
   * @param id  an ID identifying market data
   * @param dataType  the data type of the market data, either a single value or a time series of values
   * @return true if the market data identified by the ID and data type is present in the supplied data
   */
  private static boolean isSupplied(
      MarketDataId<?> id,
      MarketDataNode.DataType dataType,
      ScenarioMarketData suppliedData) {

    switch (dataType) {
      case TIME_SERIES:
        return (id instanceof ObservableId) && !suppliedData.getTimeSeries((ObservableId) id).isEmpty();
      case SINGLE_VALUE:
        return suppliedData.containsValue(id);
      default:
        throw new IllegalArgumentException("Unexpected data type " + dataType);
    }
  }
}
