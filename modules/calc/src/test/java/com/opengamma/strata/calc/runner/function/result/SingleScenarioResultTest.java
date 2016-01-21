/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link SingleScenarioResult}.
 */
@Test
public class SingleScenarioResultTest {

  public void create() {
    SingleScenarioResult<String> test = SingleScenarioResult.of(3, "A");
    assertEquals(test.getScenarioCount(), 3);
    assertEquals(test.getResult(), "A");
    assertEquals(test.get(0), "A");
    assertEquals(test.get(1), "A");
    assertEquals(test.get(2), "A");
    assertEquals(test.stream().collect(toList()), ImmutableList.of("A", "A", "A"));
  }

  public void coverage() {
    SingleScenarioResult<String> test = SingleScenarioResult.of(3, "A");
    coverImmutableBean(test);
    SingleScenarioResult<String> test2 = SingleScenarioResult.of(2, "B");
    coverBeanEquals(test, test2);
  }

}
