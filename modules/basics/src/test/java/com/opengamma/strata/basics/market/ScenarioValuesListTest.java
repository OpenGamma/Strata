/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

@Test
public class ScenarioValuesListTest {

  public void of() {
    assertThat(ScenarioValuesList.of(1, 2, 3)).isEqualTo(ScenarioValuesList.of(ImmutableList.of(1, 2, 3)));
  }

  public void getValue() {
    ScenarioValuesList<Integer> valuesList = ScenarioValuesList.of(1, 2, 3);
    assertThat(valuesList.getValue(0)).isEqualTo(1);
    assertThat(valuesList.getValue(1)).isEqualTo(2);
    assertThat(valuesList.getValue(2)).isEqualTo(3);
    assertThrows(() -> valuesList.getValue(-1), IllegalArgumentException.class, "Expected 0 <= 'scenarioIndex' < 3, but found -1");
    assertThrows(() -> valuesList.getValue(3), IllegalArgumentException.class, "Expected 0 <= 'scenarioIndex' < 3, but found 3");
    assertThat(valuesList.getValues()).isEqualTo(ImmutableList.of(1, 2, 3));
  }

  public void getScenarioCount() {
    ScenarioValuesList<Integer> valuesList = ScenarioValuesList.of(1, 2, 3);
    assertThat(valuesList.getScenarioCount()).isEqualTo(3);
  }
}
