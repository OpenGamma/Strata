/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

@Test
public class DefaultScenarioResultTest {

  public void create() {
    DefaultScenarioResult<Integer> test = DefaultScenarioResult.of(1, 2, 3);
    assertThat(test.getResults()).isEqualTo(ImmutableList.of(1, 2, 3));
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1);
    assertThat(test.get(1)).isEqualTo(2);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).isEqualTo(ImmutableList.of(1, 2, 3));
  }

  public void coverage() {
    DefaultScenarioResult<Integer> test = DefaultScenarioResult.of(1, 2, 3);
    coverImmutableBean(test);
    DefaultScenarioResult<String> test2 = DefaultScenarioResult.of("2", "3");
    coverBeanEquals(test, test2);
  }

}
