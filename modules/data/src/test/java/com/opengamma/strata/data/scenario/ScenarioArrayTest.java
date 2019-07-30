/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ScenarioArray}.
 */
public class ScenarioArrayTest {

  @Test
  public void test_of_array() {
    ScenarioArray<String> test = ScenarioArray.of("1", "2", "3");
    DefaultScenarioArray<String> expected = DefaultScenarioArray.of("1", "2", "3");
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_list() {
    ScenarioArray<String> test = ScenarioArray.of(ImmutableList.of("1", "2", "3"));
    DefaultScenarioArray<String> expected = DefaultScenarioArray.of("1", "2", "3");
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_function() {
    ScenarioArray<String> test = ScenarioArray.of(3, i -> Integer.toString(i + 1));
    DefaultScenarioArray<String> expected = DefaultScenarioArray.of("1", "2", "3");
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_ofSingleValue() {
    ScenarioArray<String> test = ScenarioArray.ofSingleValue(3, "aaa");
    SingleScenarioArray<String> expected = SingleScenarioArray.of(3, "aaa");
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_stream() {
    ScenarioArray<String> test = new ScenarioArray<String>() {

      @Override
      public int getScenarioCount() {
        return 3;
      }

      @Override
      public String get(int scenarioIndex) {
        return "" + scenarioIndex;
      }

    };
    List<String> output = test.stream().collect(toImmutableList());
    assertThat(output).containsExactly("0", "1", "2");
  }

}
