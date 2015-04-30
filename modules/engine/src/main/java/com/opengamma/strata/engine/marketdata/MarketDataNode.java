/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * A node in a tree of dependencies of market data required by a set of calculations.
 * <p>
 * The immediate children of the root node are the market data values required by the calculations. Their child
 * nodes are the market data they depend on, and so on.
 * <p>
 * This tree is used to determine the order in which market data is built. The leaves of the tree
 * represent market data with no dependencies. This can be observable data whose value can be provided
 * by a market data system or a value that is supplied by the user or has already be built by
 * the engine.
 * <p>
 * For example, if a function requests a curve, there will be a node below the root representing the curve.
 * The curve's node will have a child node representing the curve group containing the curve. The curve group node
 * has child nodes representing the market data values at each of the curve points. It might also have a child node
 * representing another curve, or possibly an FX rate, and the curve and FX rate nodes would themselves
 * depend on market data values.
 */
class MarketDataNode {

  /** The type of market data represented by the node, either a single value or a time series of values. */
  enum DataType {

    /** The node represents a single market data value. */
    SINGLE_VALUE,

    /** The node represents a time series of market data values. */
    TIME_SERIES
  }

  /** The ID of the required market data. */
  private final MarketDataId<?> id;

  /** The type of the required market data. */
  private final DataType dataType;

  /** Child nodes identifying the market data required to build the market data in this node. */
  private final List<MarketDataNode> children;

  /**
   * Builds a tree representing the dependencies between items of market data and returns the root node.
   *
   * @param requirements  IDs of the market data that must be provided
   * @param suppliedData  data supplied by the user
   * @param builders  builders for market data, keyed by the type of market data ID they handle
   * @return the root node of the market data dependency tree
   */
  static MarketDataNode buildDependencyTree(
      MarketDataRequirements requirements,
      BaseMarketData suppliedData,
      Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders) {

    return MarketDataNode.root(children(requirements, suppliedData, builders));
  }

  /**
   * Returns child nodes representing the dependencies of an item of market data.
   *
   * @param requirements  the market data requirements for which nodes will be built
   * @param suppliedData  the market data supplied by the user
   * @param builders  builders for market data, keyed by the type of market data ID they can handle
   * @return child nodes representing the dependencies of an item of market data
   */
  private static List<MarketDataNode> children(
      MarketDataRequirements requirements,
      BaseMarketData suppliedData,
      Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders) {

    List<MarketDataNode> observableNodes =
        buildNodes(requirements.getObservables(), DataType.SINGLE_VALUE, suppliedData, builders);

    List<MarketDataNode> nonObservableNodes =
        buildNodes(requirements.getNonObservables(), DataType.SINGLE_VALUE, suppliedData, builders);

    List<MarketDataNode> timeSeriesNodes =
        buildNodes(requirements.getTimeSeries(), DataType.TIME_SERIES, suppliedData, builders);

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
   * @param suppliedData  the set of market data supplied by the user
   * @param builders  builders for building market data
   * @return market data nodes for the IDs
   */
  private static List<MarketDataNode> buildNodes(
      Set<? extends MarketDataId<?>> ids,
      DataType dataType,
      BaseMarketData suppliedData,
      Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders) {

    return ids.stream()
        .map(id -> buildNode(id, dataType, suppliedData, builders))
        .collect(toImmutableList());
  }

  /**
   * Builds a node for a market data ID.
   *
   * @param id  the ID
   * @param dataType  the type of data represented by the ID, either a single value or a time series of values
   * @param suppliedData  the set of market data supplied by the user
   * @param builders  builders for building market data
   * @return a market data node for the ID
   */
  private static MarketDataNode buildNode(
      MarketDataId<?> id,
      DataType dataType,
      BaseMarketData suppliedData,
      Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders) {

    // Observable data has special handling and is guaranteed to have a builder.
    // Supplied data definitely has no dependencies because it already exists and doesn't need to be built.
    if (id instanceof ObservableId || isSupplied(id, dataType, suppliedData)) {
      return MarketDataNode.child(id, dataType);
    }
    // Find the builder that can build the data identified by the ID
    MarketDataBuilder builder = builders.get(id.getClass());

    if (builder != null) {
      @SuppressWarnings("unchecked")
      MarketDataRequirements requirements = builder.requirements(id);
      return MarketDataNode.child(id, dataType, children(requirements, suppliedData, builders));
    } else {
      // If there is no builder insert a node with no children. It will be flagged as an error when the data is built
      return MarketDataNode.child(id, dataType);
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
      DataType dataType,
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

  /**
   * Returns a root node which doesn't have a market data ID or data type.
   *
   * @param children  the child nodes representing the market data dependencies of the root node
   * @return a root node which doesn't have a market data ID or data type
   */
  static MarketDataNode root(List<MarketDataNode> children) {
    ArgChecker.notNull(children, "children");
    return new MarketDataNode(null, null, children);
  }

  /**
   * Returns a child node representing an item of market data.
   *
   * @param id  an ID identifying the market data represented by the node
   * @param dataType  the type of market data represented by the node, either a single value or a time series of values
   * @param children  the child nodes representing the market data dependencies of the node
   * @return a child node representing an item of market data
   */
  static MarketDataNode child(MarketDataId<?> id, DataType dataType, List<MarketDataNode> children) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(dataType, "dataType");
    ArgChecker.notNull(children, "children");
    return new MarketDataNode(id, dataType, children);
  }

  /**
   * Returns a child node representing an item of market data.
   *
   * @param id  an ID identifying the market data represented by the node
   * @param dataType  the type of market data represented by the node, either a single value or a time series of values
   * @return a child node representing an item of market data
   */
  static MarketDataNode child(MarketDataId<?> id, DataType dataType) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(dataType, "dataType");
    return new MarketDataNode(id, dataType, ImmutableList.of());
  }

  private MarketDataNode(MarketDataId<?> id, DataType dataType, List<MarketDataNode> children) {
    this.dataType = dataType;
    this.id = id;
    this.children = ImmutableList.copyOf(children);
  }

  /**
   * Returns a copy of the dependency tree without the leaf nodes. It also returns the market data requirements
   * represented by the leaf nodes.
   * <p>
   * The leaf nodes represent market data with no missing requirements for market data. This includes:
   * <ul>
   *   <li>Market data that is already available</li>
   *   <li>Observable data whose value can be obtained from a market data provider</li>
   *   <li>Market data that can be built from data that is already available</li>
   * </ul>
   * Therefore the market data represented by the leaf nodes can be built immediately.
   *
   * @return a copy of the dependency tree without the leaf nodes and the market data requirements
   *   represented by the leaf nodes
   */
  Pair<MarketDataNode, MarketDataRequirements> withLeavesRemoved() {
    ImmutableList.Builder<MarketDataNode> childNodesBuilder = ImmutableList.builder();
    MarketDataRequirementsBuilder requirementsBuilder = MarketDataRequirements.builder();

    for (MarketDataNode child : children) {
      if (child.isLeaf()) {
        switch (child.dataType) {
          case SINGLE_VALUE:
            requirementsBuilder.addValues(child.id);
            break;
          case TIME_SERIES:
            requirementsBuilder.addTimeSeries(((ObservableId) child.id));
            break;
        }
      } else {
        Pair<MarketDataNode, MarketDataRequirements> childResult = child.withLeavesRemoved();
        childNodesBuilder.add(childResult.getFirst());
        requirementsBuilder.addRequirements(childResult.getSecond());
      }
    }
    MarketDataNode node = new MarketDataNode(id, dataType, childNodesBuilder.build());
    MarketDataRequirements requirements = requirementsBuilder.build();

    return Pair.of(node, requirements);
  }

  /**
   * Returns true if this node has no children.
   *
   * @return true if this node has no children
   */
  boolean isLeaf() {
    return children.isEmpty();
  }

  /**
   * Prints this node and its tree of children to an ASCII tree.
   *
   * @param builder  a string builder into which the result will be written
   * @param indent  the indent printed at the start of the line before the node
   * @param childIndent  the indent printed at the start of the line before the node's children
   * @return the string builder containing the pretty-printed tree
   */
  private StringBuilder prettyPrint(StringBuilder builder, String indent, String childIndent) {
    String nodeDescription = (id == null) ? "Root" : (id + " " + dataType);
    builder.append('\n').append(indent).append(nodeDescription);

    for (Iterator<MarketDataNode> it = children.iterator(); it.hasNext(); ) {
      MarketDataNode child = it.next();
      String newIndent;
      String newChildIndent;
      boolean isFinalChild = !it.hasNext();

      if (!isFinalChild) {
        newIndent = childIndent + " |--";  // Unicode boxes: \u251c\u2500\u2500
        newChildIndent = childIndent + " |  ";  // Unicode boxes: \u2502
      } else {
        newIndent = childIndent + " `--";  // Unicode boxes: \u2514\u2500\u2500
        newChildIndent = childIndent + "    ";
      }
      child.prettyPrint(builder, newIndent, newChildIndent);
    }
    return builder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarketDataNode that = (MarketDataNode) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(dataType, that.dataType) &&
        Objects.equals(children, that.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, dataType, children);
  }

  @Override
  public String toString() {
    return prettyPrint(new StringBuilder(), "", "").toString();
  }
}
