/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

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
   * Tests building a tree of requirements using market data functions.
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

    // Requirements for each item in the tree - used to initialize the functions
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

    TestMarketDataFunctionA builderA = new TestMarketDataFunctionA();
    TestMarketDataFunctionB builderB = new TestMarketDataFunctionB(reqsMap);

    ImmutableMap<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions =
        ImmutableMap.of(
            TestIdA.class, builderA,
            TestIdB.class, builderB);

    MarketDataNode root =
        MarketDataNode.buildDependencyTree(
            requirements,
            BuiltScenarioMarketData.empty(),
            MarketDataConfig.empty(),
            functions);

    assertThat(root).isEqualTo(expected);
  }

  /**
   * Tests that supplied data is in a leaf node and the functions aren't asked for dependencies for supplied data.
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

    TestMarketDataFunctionB builder = new TestMarketDataFunctionB(reqsMap);

    ImmutableMap<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions =
        ImmutableMap.of(
            TestIdB.class, builder);

    MarketDataNode root1 =
        MarketDataNode.buildDependencyTree(
            requirements,
            BuiltScenarioMarketData.empty(),
            MarketDataConfig.empty(),
            functions);

    assertThat(root1).isEqualTo(expected1);

    BuiltScenarioMarketData suppliedData =
        BuiltScenarioMarketData.builder(date(2011, 3, 8))
            .addValue(new TestIdB("1"), new TestMarketDataB())
            .addValue(new TestIdB("3"), new TestMarketDataB())
            .build();

    MarketDataNode root2 =
        MarketDataNode.buildDependencyTree(
            requirements,
            suppliedData,
            MarketDataConfig.empty(),
            functions);

    MarketDataNode expected2 =
        rootNode(
            valueNode(new TestIdB("1")),
            valueNode(new TestIdB("3")));

    assertThat(root2).isEqualTo(expected2);
  }

  /**
   * Test a node with no children is added when there is no market data function for an ID.
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

    TestMarketDataFunctionB builder = new TestMarketDataFunctionB(ImmutableMap.of(new TestIdB("2"), id2Reqs));
    ImmutableMap<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions =
        ImmutableMap.of(TestIdB.class, builder);
    // Build the tree without providing a market data function to handle TestId3
    MarketDataNode root =
        MarketDataNode.buildDependencyTree(
            requirements,
            BuiltScenarioMarketData.empty(),
            MarketDataConfig.empty(),
            functions);

    assertThat(root).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  private static MarketDataNode rootNode(MarketDataNode... children) {
    return MarketDataNode.root(Arrays.asList(children));
  }

  private static MarketDataNode valueNode(MarketDataId<?> id, MarketDataNode... children) {
    return MarketDataNode.child(id, MarketDataNode.DataType.SINGLE_VALUE, Arrays.asList(children));
  }

  private static MarketDataNode observableNode(ObservableId id) {
    return MarketDataNode.leaf(id, MarketDataNode.DataType.SINGLE_VALUE);
  }

  private static MarketDataNode timeSeriesNode(ObservableId id) {
    return MarketDataNode.leaf(id, MarketDataNode.DataType.TIME_SERIES);
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
    public ObservableSource getObservableSource() {
      return ObservableSource.NONE;
    }

    @Override
    public ObservableId withObservableSource(ObservableSource obsSource) {
      return this;
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

  private static final class TestMarketDataFunctionA implements MarketDataFunction<Double, TestIdA> {

    @Override
    public MarketDataRequirements requirements(TestIdA id, MarketDataConfig marketDataConfig) {
      // The ID represents observable data which has no dependencies by definition
      return MarketDataRequirements.empty();
    }

    @Override
    public MarketDataBox<Double> build(
        TestIdA id,
        MarketDataConfig marketDataConfig,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      throw new UnsupportedOperationException("build not implemented");
    }

    @Override
    public Class<TestIdA> getMarketDataIdType() {
      return TestIdA.class;
    }
  }

  private static final class TestMarketDataB {
  }

  private static final class TestMarketDataFunctionB implements MarketDataFunction<TestMarketDataB, TestIdB> {

    private final Map<TestIdB, MarketDataRequirements> requirements;

    private TestMarketDataFunctionB(Map<TestIdB, MarketDataRequirements> requirements) {
      this.requirements = requirements;
    }

    @Override
    public MarketDataRequirements requirements(TestIdB id, MarketDataConfig marketDataConfig) {
      return requirements.getOrDefault(id, MarketDataRequirements.empty());
    }

    @Override
    public MarketDataBox<TestMarketDataB> build(
        TestIdB id,
        MarketDataConfig marketDataConfig,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      throw new UnsupportedOperationException("build not implemented");
    }

    @Override
    public Class<TestIdB> getMarketDataIdType() {
      return TestIdB.class;
    }
  }
}
