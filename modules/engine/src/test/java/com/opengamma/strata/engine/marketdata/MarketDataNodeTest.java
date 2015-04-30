/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.marketdata.id.FieldName;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

@Test
public class MarketDataNodeTest {

  /**
   * Tests removing the leaf dependencies from the tree to decide which market data can be built.
   */
  public void withLeavesRemoved() {
    MarketDataNode root =
        rootNode(
            observableNode(new TestIdA("1")),
            valueNode(
                new TestIdB("2"),
                valueNode(new TestIdB("3")),
                observableNode(new TestIdA("4")),
                valueNode(
                    new TestIdB("5"),
                    timeSeriesNode(new TestIdA("6")))),
            valueNode(new TestIdB("7")));

    Pair<MarketDataNode, MarketDataRequirements> pair1 = root.withLeavesRemoved();

    MarketDataRequirements expectedReqs1 =
        MarketDataRequirements.builder()
            .addValues(new TestIdA("1"))
            .addValues(new TestIdB("3"))
            .addValues(new TestIdA("4"))
            .addTimeSeries(new TestIdA("6"))
            .addValues(new TestIdB("7"))
            .build();

    MarketDataNode expectedTree1 =
        rootNode(
            valueNode(
                new TestIdB("2"),
                valueNode(
                    new TestIdB("5"))));

    MarketDataNode tree1 = pair1.getFirst();
    MarketDataRequirements reqs1 = pair1.getSecond();

    assertThat(tree1).isEqualTo(expectedTree1);
    assertThat(expectedReqs1).isEqualTo(reqs1);

    Pair<MarketDataNode, MarketDataRequirements> pair2 = tree1.withLeavesRemoved();

    MarketDataRequirements expectedReqs2 =
        MarketDataRequirements.builder()
            .addValues(new TestIdB("5"))
            .build();

    MarketDataNode expectedTree2 =
        rootNode(
            valueNode(
                new TestIdB("2")));

    MarketDataNode tree2 = pair2.getFirst();
    MarketDataRequirements reqs2 = pair2.getSecond();

    assertThat(tree2).isEqualTo(expectedTree2);
    assertThat(expectedReqs2).isEqualTo(reqs2);

    Pair<MarketDataNode, MarketDataRequirements> pair3 = tree2.withLeavesRemoved();

    MarketDataRequirements expectedReqs3 =
        MarketDataRequirements.builder()
            .addValues(new TestIdB("2"))
            .build();

    MarketDataNode tree3 = pair3.getFirst();
    MarketDataRequirements reqs3 = pair3.getSecond();

    assertThat(tree3.isLeaf()).isTrue();
    assertThat(expectedReqs3).isEqualTo(reqs3);
  }

  /**
   * Tests building a tree of requirements using market data builders.
   */
  public void buildDependencyTree() {
    MarketDataNode expected =
        rootNode(
            observableNode(new TestIdA("1")),
            valueNode(
                new TestIdB("2"),
                valueNode(
                    new TestIdB("4"),
                    observableNode(new TestIdA("5"))),
                timeSeriesNode(new TestIdA("3"))),
            timeSeriesNode(new TestIdA("6")));

    // The requirements for the data directly used by the calculations
    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .addValues(new TestIdA("1"), new TestIdB("2"))
            .addTimeSeries(new TestIdA("6"))
            .build();

    // Requirements for each item in the tree - used to initialize the builders
    MarketDataRequirements id2Reqs =
        MarketDataRequirements.builder()
            .addTimeSeries(new TestIdA("3"))
            .addValues(new TestIdB("4"))
            .build();

    MarketDataRequirements id4Reqs =
        MarketDataRequirements.builder()
            .addValues(new TestIdA("5"))
            .build();

    ImmutableMap<TestIdB, MarketDataRequirements> reqsMap =
        ImmutableMap.of(
            new TestIdB("2"), id2Reqs,
            new TestIdB("4"), id4Reqs);

    TestMarketDataBuilderA builderA = new TestMarketDataBuilderA();
    TestMarketDataBuilderB builderB = new TestMarketDataBuilderB(reqsMap);

    ImmutableMap<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders =
        ImmutableMap.of(
            TestIdA.class, builderA,
            TestIdB.class, builderB);

    MarketDataNode root =
        MarketDataNode.buildDependencyTree(requirements, BaseMarketData.empty(date(2011, 3, 8)), builders);

    assertThat(root).isEqualTo(expected);
  }

  /**
   * Tests that supplied data is in a leaf node and the builders aren't asked for dependencies for supplied data.
   */
  public void noDependenciesForSuppliedData() {
    MarketDataNode expected1 =
        rootNode(
            valueNode(
                new TestIdB("1"),
                observableNode(new TestIdA("2"))),
            valueNode(
                new TestIdB("3"),
                valueNode(new TestIdB("4"))));

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .addValues(new TestIdB("1"), new TestIdB("3"))
            .build();

    MarketDataRequirements id1Reqs =
        MarketDataRequirements.builder()
            .addValues(new TestIdA("2"))
            .build();

    MarketDataRequirements id3Reqs =
        MarketDataRequirements.builder()
            .addValues(new TestIdB("4"))
            .build();

    ImmutableMap<TestIdB, MarketDataRequirements> reqsMap =
        ImmutableMap.of(
            new TestIdB("1"), id1Reqs,
            new TestIdB("3"), id3Reqs);

    TestMarketDataBuilderB builder = new TestMarketDataBuilderB(reqsMap);

    ImmutableMap<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders =
        ImmutableMap.of(
            TestIdB.class, builder);

    MarketDataNode root1 =
        MarketDataNode.buildDependencyTree(requirements, BaseMarketData.empty(date(2011, 3, 8)), builders);

    assertThat(root1).isEqualTo(expected1);

    BaseMarketData suppliedData =
        BaseMarketData.builder(date(2011, 3, 8))
            .addValue(new TestIdB("1"), new TestMarketDataB())
            .addValue(new TestIdB("3"), new TestMarketDataB())
            .build();

    MarketDataNode root2 = MarketDataNode.buildDependencyTree(requirements, suppliedData, builders);

    MarketDataNode expected2 =
        rootNode(
            valueNode(new TestIdB("1")),
            valueNode(new TestIdB("3")));

    assertThat(root2).isEqualTo(expected2);
  }

  /**
   * Test a node with no children is added when there is no market data builder for an ID.
   */
  public void noMarketDataBuilder() {
    MarketDataNode expected =
        rootNode(
            valueNode(new TestIdC("1")),
            valueNode(
                new TestIdB("2"),
                valueNode(new TestIdC("3"))));

    MarketDataRequirements requirements =
        MarketDataRequirements.builder()
            .addValues(new TestIdC("1"), new TestIdB("2"))
            .build();

    MarketDataRequirements id2Reqs =
        MarketDataRequirements.builder()
            .addValues(new TestIdC("3"))
            .build();

    TestMarketDataBuilderB builder = new TestMarketDataBuilderB(ImmutableMap.of(new TestIdB("2"), id2Reqs));
    ImmutableMap<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders =
        ImmutableMap.of(TestIdB.class, builder);
    // Build the tree without providing a market data builder to handle TestId3
    MarketDataNode root =
        MarketDataNode.buildDependencyTree(requirements, BaseMarketData.empty(date(2011, 8, 3)), builders);

    assertThat(root).isEqualTo(expected);
  }

  //------------------------------------------------------------------------------------------

  private static MarketDataNode rootNode(MarketDataNode... children) {
    return MarketDataNode.root(Arrays.asList(children));
  }

  private static MarketDataNode valueNode(MarketDataId id, MarketDataNode... children) {
    return MarketDataNode.child(id, MarketDataNode.DataType.SINGLE_VALUE, Arrays.asList(children));
  }

  private static MarketDataNode observableNode(ObservableId id) {
    return MarketDataNode.child(id, MarketDataNode.DataType.SINGLE_VALUE);
  }

  private static MarketDataNode timeSeriesNode(ObservableId id) {
    return MarketDataNode.child(id, MarketDataNode.DataType.TIME_SERIES);
  }

  class TestIdA implements ObservableId {

    private final StandardId id;

    TestIdA(String id) {
      this.id = StandardId.of("test", id);
    }

    @Override
    public StandardId getStandardId() {
      return id;
    }

    @Override
    public FieldName getFieldName() {
      return FieldName.MARKET_VALUE;
    }

    @Override
    public MarketDataFeed getMarketDataFeed() {
      return MarketDataFeed.NONE;
    }

    @Override
    public Class<Double> getMarketDataType() {
      return Double.class;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestIdA idA = (TestIdA) o;
      return Objects.equals(id, idA.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "TestId1 [id=" + id + "]";
    }
  }

  class TestIdB implements MarketDataId<TestMarketDataB> {

    private final String str;

    TestIdB(String str) {
      this.str = str;
    }

    @Override
    public Class<TestMarketDataB> getMarketDataType() {
      return TestMarketDataB.class;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestIdB idB = (TestIdB) o;
      return Objects.equals(str, idB.str);
    }

    @Override
    public int hashCode() {
      return Objects.hash(str);
    }

    @Override
    public String toString() {
      return "TestId2 [str='" + str + "']";
    }
  }

  private static final class TestIdC implements MarketDataId<String> {

    private final String id;

    private TestIdC(String id) {
      this.id = id;
    }

    @Override
    public Class<String> getMarketDataType() {
      return String.class;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestIdC idC = (TestIdC) o;
      return Objects.equals(id, idC.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "BazId [id='" + id + "']";
    }
  }

  private static final class TestMarketDataBuilderA implements MarketDataBuilder<Double, TestIdA> {

    @Override
    public MarketDataRequirements requirements(TestIdA id) {
      // The ID represents observable data which has no dependencies by definition
      return MarketDataRequirements.EMPTY;
    }

    @Override
    public Result<Double> build(TestIdA id, BaseMarketData builtData) {
      throw new UnsupportedOperationException("build not implemented");
    }

    @Override
    public Class<TestIdA> getMarketDataIdType() {
      return TestIdA.class;
    }
  }

  private static final class TestMarketDataB { }

  private static final class TestMarketDataBuilderB implements MarketDataBuilder<TestMarketDataB, TestIdB> {

    private final Map<TestIdB, MarketDataRequirements> requirements;

    private TestMarketDataBuilderB(Map<TestIdB, MarketDataRequirements> requirements) {
      this.requirements = requirements;
    }

    @Override
    public MarketDataRequirements requirements(TestIdB id) {
      return requirements.getOrDefault(id, MarketDataRequirements.EMPTY);
    }

    @Override
    public Result<TestMarketDataB> build(TestIdB id, BaseMarketData builtData) {
      throw new UnsupportedOperationException("build not implemented");
    }

    @Override
    public Class<TestIdB> getMarketDataIdType() {
      return TestIdB.class;
    }
  }
}
