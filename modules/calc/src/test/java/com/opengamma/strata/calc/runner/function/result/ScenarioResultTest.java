/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ScenarioResult}.
 */
@Test
public class ScenarioResultTest {

  public void test_of_array() {
    ScenarioResult<String> test = ScenarioResult.of("1", "2", "3");
    DefaultScenarioResult<String> expected = DefaultScenarioResult.of("1", "2", "3");
    assertEquals(test, expected);
  }

  public void test_of_list() {
    ScenarioResult<String> test = ScenarioResult.of(ImmutableList.of("1", "2", "3"));
    DefaultScenarioResult<String> expected = DefaultScenarioResult.of("1", "2", "3");
    assertEquals(test, expected);
  }

  public void test_of_function() {
    ScenarioResult<String> test = ScenarioResult.of(3, i -> Integer.toString(i + 1));
    DefaultScenarioResult<String> expected = DefaultScenarioResult.of("1", "2", "3");
    assertEquals(test, expected);
  }

}
