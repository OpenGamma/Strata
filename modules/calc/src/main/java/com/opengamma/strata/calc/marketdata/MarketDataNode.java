/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * A node in a tree of dependencies of market data required by a set of calculations.
 * <p>
 * The immediate children of the root node are the market data values required by the calculations. Their child
 * nodes are the market data they depend on, and so on.
 * <p>
 * This tree is used to determine the order in which market data is built. The leaves of the tree
 * represent market data with no dependencies. This includes:
 * <ul>
 *   <li>Market data that is already available</li>
 *   <li>Observable data whose value can be obtained from a market data provider</li>
 *   <li>Market data that can be built from data that is already available</li>
 * </ul>
 * For example, if a function requests a curve, there will be a node below the root representing the curve.
 * The curve's node will have a child node representing the curve group containing the curve. The curve group node
 * has child nodes representing the market data values at each of the curve points. It might also have a child node
 * representing another curve, or possibly an FX rate, and the curve and FX rate nodes would themselves
 * depend on market data values.
 */
final class MarketDataNode {

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
  private final List<MarketDataNode> dependencies;

  /**
   * Builds a tree representing the dependencies between items of market data and returns the root node.
   *
   * @param requirements  IDs of the market data that must be provided
   * @param suppliedData  data supplied by the user
   * @param marketDataConfig  configuration specifying how market data values should be built
   * @param functions  functions for market data, keyed by the type of market data ID they handle
   * @return the root node of the market data dependency tree
   */
  static MarketDataNode buildDependencyTree(
      MarketDataRequirements requirements,
      ScenarioMarketData suppliedData,
      MarketDataConfig marketDataConfig,
      Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions) {

    DependencyTreeBuilder treeBuilder = DependencyTreeBuilder.of(suppliedData, requirements, marketDataConfig, functions);
    return MarketDataNode.root(treeBuilder.dependencyNodes());
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
   * Returns a leaf node representing an item of market data with no dependencies on other market data.
   *
   * @param id  an ID identifying the market data represented by the node
   * @param dataType  the type of market data represented by the node, either a single value or a time series of values
   * @return a leaf node representing an item of market data with no dependencies on other market data
   */
  static MarketDataNode leaf(MarketDataId<?> id, DataType dataType) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(dataType, "dataType");
    return new MarketDataNode(id, dataType, ImmutableList.of());
  }

  private MarketDataNode(MarketDataId<?> id, DataType dataType, List<MarketDataNode> dependencies) {
    this.dataType = dataType;
    this.id = id;
    this.dependencies = ImmutableList.copyOf(dependencies);
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

    for (MarketDataNode child : dependencies) {
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
    return dependencies.isEmpty();
  }

  /**
   * Returns the ID of the market data value represented by this node.
   *
   * @return the ID of the market data value represented by this node
   */
  public MarketDataId<?> getId() {
    return id;
  }

  /**
   * Prints this node and its tree of dependencies to an ASCII tree.
   *
   * @param builder  a string builder into which the result will be written
   * @param indent  the indent printed at the start of the line before the node
   * @param childIndent  the indent printed at the start of the line before the node's children
   * @return the string builder containing the pretty-printed tree
   */
  private StringBuilder prettyPrint(StringBuilder builder, String indent, String childIndent) {
    String nodeDescription = (id == null) ? "Root" : (id + " " + dataType);
    builder.append('\n').append(indent).append(nodeDescription);

    for (Iterator<MarketDataNode> it = dependencies.iterator(); it.hasNext();) {
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
        Objects.equals(dependencies, that.dependencies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, dataType, dependencies);
  }

  @Override
  public String toString() {
    return prettyPrint(new StringBuilder(), "", "").toString();
  }
}
