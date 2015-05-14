/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

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
class DependencyTreeBuilder {

  /** The market data supplied by the user. */
  private final BaseMarketData suppliedData;

  /** The builders that create items of market data. */
  private final Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders;

  /** The requirements for market data used in a set of calculations. */
  private final MarketDataRequirements requirements;

  /**
   * Returns a tree builder that builds the dependency tree for the market data required by a set of calculations.
   *
   * @param suppliedData  market data supplied by the user
   * @param requirements  specifies the market data required for the calculations
   * @param builders  builders that create items of market data
   * @return a tree builder that builds the dependency tree for the market data required by a set of calculations
   */
  static DependencyTreeBuilder of(
      BaseMarketData suppliedData,
      MarketDataRequirements requirements,
      Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders) {

    return new DependencyTreeBuilder(suppliedData, requirements, builders);
  }

  private DependencyTreeBuilder(
      BaseMarketData suppliedData,
      MarketDataRequirements requirements,
      Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders) {

    this.suppliedData = suppliedData;
    this.requirements = requirements;
    this.builders = builders;
  }

  /**
   * Returns nodes representing the dependencies of the market data required for a set of calculations.
   *
   * @return nodes representing the dependencies of the market data required for a set of calculations
   */
  List<MarketDataNode> childNodes() {
    return childNodes(requirements);
  }

  /**
   * Returns child nodes representing the dependencies of a set of market data.
   *
   * @param requirements  requirements for market data needed for a set of calculations
   * @return child nodes representing the dependencies of a set of market data
   */
  private List<MarketDataNode> childNodes(MarketDataRequirements requirements) {

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

    // Observable data has special handling and is guaranteed to have a builder.
    // Supplied data definitely has no dependencies because it already exists and doesn't need to be built.
    if (id instanceof ObservableId || isSupplied(id, dataType, suppliedData)) {
      return MarketDataNode.leaf(id, dataType);
    }
    // Find the builder that can build the data identified by the ID
    MarketDataBuilder builder = builders.get(id.getClass());

    if (builder != null) {
      @SuppressWarnings("unchecked")
      MarketDataRequirements requirements = builder.requirements(id);
      return MarketDataNode.child(id, dataType, childNodes(requirements));
    } else {
      // If there is no builder insert a leaf node. It will be flagged as an error when the data is built
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
      BaseMarketData suppliedData) {

    switch (dataType) {
      case TIME_SERIES:
        return (id instanceof ObservableId) && suppliedData.containsTimeSeries((ObservableId) id);
      case SINGLE_VALUE:
        return suppliedData.containsValue(id);
      default:
        throw new IllegalArgumentException("Unexpected data type " + dataType);
    }
  }
}
