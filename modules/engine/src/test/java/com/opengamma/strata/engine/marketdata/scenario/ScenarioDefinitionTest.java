/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.scenario;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.Perturbation;

@Test
public class ScenarioDefinitionTest {

  private static final TestFilter FILTER_A = new TestFilter("a");
  private static final TestFilter FILTER_B = new TestFilter("b");
  private static final TestFilter FILTER_C = new TestFilter("c");
  private static final TestPerturbation PERTURBATION_A1 = new TestPerturbation(1);
  private static final TestPerturbation PERTURBATION_A2 = new TestPerturbation(2);
  private static final TestPerturbation PERTURBATION_B1 = new TestPerturbation(3);
  private static final TestPerturbation PERTURBATION_B2 = new TestPerturbation(4);
  private static final TestPerturbation PERTURBATION_C1 = new TestPerturbation(5);
  private static final TestPerturbation PERTURBATION_C2 = new TestPerturbation(6);

  private static final PerturbationMapping<Object> MAPPING_A =
      PerturbationMapping.of(Object.class, FILTER_A, PERTURBATION_A1, PERTURBATION_A2);

  private static final PerturbationMapping<Object> MAPPING_B =
      PerturbationMapping.of(Object.class, FILTER_B, PERTURBATION_B1, PERTURBATION_B2);

  private static final PerturbationMapping<Object> MAPPING_C =
      PerturbationMapping.of(Object.class, FILTER_C, PERTURBATION_C1, PERTURBATION_C2);

  public void ofMappings() {
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mappings);
    List<String> scenarioNames = ImmutableList.of("Scenario 1", "Scenario 2");
    assertThat(scenarioDefinition.getMappings()).isEqualTo(mappings);
    assertThat(scenarioDefinition.getScenarioNames()).isEqualTo(scenarioNames);
  }

  public void ofMappingsWithNames() {
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    List<String> scenarioNames = ImmutableList.of("foo", "bar");
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mappings, scenarioNames);
    assertThat(scenarioDefinition.getMappings()).isEqualTo(mappings);
    assertThat(scenarioDefinition.getScenarioNames()).isEqualTo(scenarioNames);
  }

  public void allCombinationsOf() {
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofAllCombinations(mappings);
    List<String> scenarioNames =
        ImmutableList.of(
            "Scenario 1",
            "Scenario 2",
            "Scenario 3",
            "Scenario 4",
            "Scenario 5",
            "Scenario 6",
            "Scenario 7",
            "Scenario 8");
    List<PerturbationMapping<Object>> expectedMappings =
        ImmutableList.of(
            PerturbationMapping.of(
                Object.class,
                FILTER_A,
                PERTURBATION_A1,
                PERTURBATION_A2,
                PERTURBATION_A1,
                PERTURBATION_A2,
                PERTURBATION_A1,
                PERTURBATION_A2,
                PERTURBATION_A1,
                PERTURBATION_A2),
            PerturbationMapping.of(
                Object.class,
                FILTER_B,
                PERTURBATION_B1,
                PERTURBATION_B1,
                PERTURBATION_B2,
                PERTURBATION_B2,
                PERTURBATION_B1,
                PERTURBATION_B1,
                PERTURBATION_B2,
                PERTURBATION_B2),
            PerturbationMapping.of(
                Object.class,
                FILTER_C,
                PERTURBATION_C1,
                PERTURBATION_C1,
                PERTURBATION_C1,
                PERTURBATION_C1,
                PERTURBATION_C2,
                PERTURBATION_C2,
                PERTURBATION_C2,
                PERTURBATION_C2));
    assertThat(scenarioDefinition.getMappings()).isEqualTo(expectedMappings);
    assertThat(scenarioDefinition.getScenarioNames()).isEqualTo(scenarioNames);
  }

  public void allCombinationsOfWithNames() {
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    List<String> scenarioNames = ImmutableList.of("foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8");
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofAllCombinations(mappings, scenarioNames);
    List<PerturbationMapping<?>> expectedMappings =
        ImmutableList.of(
            PerturbationMapping.of(
                Object.class,
                FILTER_A,
                PERTURBATION_A1,
                PERTURBATION_A2,
                PERTURBATION_A1,
                PERTURBATION_A2,
                PERTURBATION_A1,
                PERTURBATION_A2,
                PERTURBATION_A1,
                PERTURBATION_A2),
            PerturbationMapping.of(
                Object.class,
                FILTER_B,
                PERTURBATION_B1,
                PERTURBATION_B1,
                PERTURBATION_B2,
                PERTURBATION_B2,
                PERTURBATION_B1,
                PERTURBATION_B1,
                PERTURBATION_B2,
                PERTURBATION_B2),
            PerturbationMapping.of(
                Object.class,
                FILTER_C,
                PERTURBATION_C1,
                PERTURBATION_C1,
                PERTURBATION_C1,
                PERTURBATION_C1,
                PERTURBATION_C2,
                PERTURBATION_C2,
                PERTURBATION_C2,
                PERTURBATION_C2));
    assertThat(scenarioDefinition.getMappings()).isEqualTo(expectedMappings);
    assertThat(scenarioDefinition.getScenarioNames()).isEqualTo(scenarioNames);
  }

  /**
   * Tests that a scenario definition won't be built if the scenarios names are specified and there
   * are the wrong number. The mappings all have 2 perturbations which should mean 2 scenarios, but
   * there are 3 scenario names.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void ofMappingsWrongNumberOfScenarioNames() {
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    List<String> scenarioNames = ImmutableList.of("foo", "bar", "baz");
    ScenarioDefinition.ofMappings(mappings, scenarioNames);
  }

  /**
   * Tests that a scenario definition won't be built if the mappings don't have the same number of perturbations
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void ofMappingsDifferentNumberOfPerturbations() {
    PerturbationMapping<Object> mappingC = PerturbationMapping.of(Object.class, FILTER_C, PERTURBATION_C1);
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, mappingC);
    ScenarioDefinition.ofMappings(mappings);
  }

  /**
   * Tests that a scenario definition won't be built if the mappings don't have the same number of perturbations
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void ofMappingsWithNamesDifferentNumberOfPerturbations() {
    PerturbationMapping<Object> mappingC = PerturbationMapping.of(Object.class, FILTER_C, PERTURBATION_C1);
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, mappingC);
    List<String> scenarioNames = ImmutableList.of("foo", "bar");
    ScenarioDefinition.ofMappings(mappings, scenarioNames);
  }

  /**
   * Tests that a scenario definition won't be built if the scenarios names are specified and there are the wrong number
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void allCombinationsOfWrongNumberOfScenarioNames() {
    List<PerturbationMapping<Object>> mappings = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    List<String> scenarioNames = ImmutableList.of("foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7");
    ScenarioDefinition.ofAllCombinations(mappings, scenarioNames);
  }

  public void repeatItems() {
    List<Integer> inputs = ImmutableList.of(1, 2, 3, 4);

    List<Integer> expected1 = ImmutableList.of(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4);
    assertThat(ScenarioDefinition.repeatItems(inputs, 12, 1)).isEqualTo(expected1);

    List<Integer> expected2 = ImmutableList.of(1, 1, 2, 2, 3, 3, 4, 4);
    assertThat(ScenarioDefinition.repeatItems(inputs, 8, 2)).isEqualTo(expected2);

    List<Integer> expected3 = ImmutableList.of(1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4);
    assertThat(ScenarioDefinition.repeatItems(inputs, 12, 3)).isEqualTo(expected3);

    List<Integer> expected4 = ImmutableList.of(1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4);
    assertThat(ScenarioDefinition.repeatItems(inputs, 24, 3)).isEqualTo(expected4);
  }

  /**
   * Tests that exceptions are thrown when the scenario names contain duplicate values.
   */
  public void nonUniqueNames() {
    List<PerturbationMapping<Object>> mappings1 = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    List<String> names1 = ImmutableList.of("foo1", "foo1", "foo3", "foo4", "foo5", "foo6", "foo5", "foo8");
    String msg1 = "Scenario names must be unique but duplicates were found: foo1, foo5";
    assertThrows(() -> ScenarioDefinition.ofAllCombinations(mappings1, names1), IllegalArgumentException.class, msg1);


    List<PerturbationMapping<Object>> mappings2 = ImmutableList.of(MAPPING_A, MAPPING_B, MAPPING_C);
    List<String> names2 = ImmutableList.of("foo", "foo");
    String msg2 = "Scenario names must be unique but duplicates were found: foo";
    assertThrows(() -> ScenarioDefinition.ofMappings(mappings2, names2), IllegalArgumentException.class, msg2);
  }

  //-----------------------------------------------------------------------------------------

  private static final class TestPerturbation implements Perturbation<Object> {

    private final int id;

    private TestPerturbation(int id) {
      this.id = id;
    }

    @Override
    public Object applyTo(Object marketData) {
      return marketData;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestPerturbation that = (TestPerturbation) o;
      return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "TestPerturbation [id=" + id + "]";
    }
  }

  private static final class TestFilter implements MarketDataFilter<Object, MarketDataId<Object>> {

    private final String name;

    private TestFilter(String name) {
      this.name = name;
    }

    @Override
    public boolean matches(MarketDataId<Object> marketDataId, Object marketData) {
      return false;
    }

    @Override
    public Class<?> getMarketDataIdType() {
      return MarketDataId.class;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestFilter that = (TestFilter) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }

    @Override
    public String toString() {
      return "TestFilter [name='" + name + "']";
    }
  }
}

