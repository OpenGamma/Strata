/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ScenarioMarketDataBox}
 */
public class ScenarioMarketDataBoxTest {

  @Test
  public void isSingleOrScenarioValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    assertThat(box.isSingleValue()).isFalse();
    assertThat(box.isScenarioValue()).isTrue();
  }

  @Test
  public void getSingleValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    assertThatIllegalStateException()
        .isThrownBy(box::getSingleValue)
        .withMessage("This box does not contain a single value");
  }

  @Test
  public void getValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    assertThat(box.getValue(0)).isEqualTo(27);
    assertThat(box.getValue(1)).isEqualTo(28);
    assertThat(box.getValue(2)).isEqualTo(29);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> box.getValue(-1))
        .withMessage("Expected 0 <= 'scenarioIndex' < 3, but found -1");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> box.getValue(3))
        .withMessage("Expected 0 <= 'scenarioIndex' < 3, but found 3");
  }

  @Test
  public void getScenarioValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    ScenarioArray<Integer> scenarioValue = box.getScenarioValue();
    assertThat(scenarioValue.getScenarioCount()).isEqualTo(3);
    assertThat(scenarioValue.get(0)).isEqualTo(27);
    assertThat(scenarioValue.get(1)).isEqualTo(28);
    assertThat(scenarioValue.get(2)).isEqualTo(29);
  }

  @Test
  public void getScenarioCount() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    assertThat(box.getScenarioCount()).isEqualTo(3);
  }

  @Test
  public void map() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    MarketDataBox<Integer> result = box.map(v -> v * 2);
    assertThat(result).isEqualTo(MarketDataBox.ofScenarioValues(54, 56, 58));
  }

  /**
   * Tests that applying a function multiple times to the value creates a box of scenario values.
   */
  @Test
  public void mapWithIndex() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    MarketDataBox<Integer> scenarioBox = box.mapWithIndex(3, (v, idx) -> v + idx);
    assertThat(scenarioBox.isScenarioValue()).isTrue();
    assertThat(scenarioBox.getScenarioCount()).isEqualTo(3);
    assertThat(scenarioBox.getValue(0)).isEqualTo(27);
    assertThat(scenarioBox.getValue(1)).isEqualTo(29);
    assertThat(scenarioBox.getValue(2)).isEqualTo(31);
  }

  /**
   * Tests that an exception is thrown when trying to apply a function multiple times with a scenario count
   * that doesn't match the scenario count of the box.
   */
  @Test
  public void mapWithIndexWrongNumberOfScenarios() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    assertThatIllegalArgumentException().isThrownBy(() -> box.mapWithIndex(4, (v, idx) -> v + idx));
  }

  @Test
  public void combineWithSingleBox() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    MarketDataBox<Integer> otherBox = MarketDataBox.ofSingleValue(15);
    MarketDataBox<Integer> resultBox = box.combineWith(otherBox, (v1, v2) -> v1 + v2);
    assertThat(resultBox.isScenarioValue()).isTrue();
    assertThat(resultBox.getScenarioCount()).isEqualTo(3);
    assertThat(resultBox.getValue(0)).isEqualTo(42);
    assertThat(resultBox.getValue(1)).isEqualTo(43);
    assertThat(resultBox.getValue(2)).isEqualTo(44);
  }

  @Test
  public void combineWithScenarioBox() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    MarketDataBox<Integer> otherBox = MarketDataBox.ofScenarioValues(15, 16, 17);
    MarketDataBox<Integer> resultBox = box.combineWith(otherBox, (v1, v2) -> v1 + v2);
    assertThat(resultBox.isScenarioValue()).isTrue();
    assertThat(resultBox.getScenarioCount()).isEqualTo(3);
    assertThat(resultBox.getValue(0)).isEqualTo(42);
    assertThat(resultBox.getValue(1)).isEqualTo(44);
    assertThat(resultBox.getValue(2)).isEqualTo(46);
  }

  @Test
  public void combineWithScenarioBoxWithWrongNumberOfScenarios() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    MarketDataBox<Integer> otherBox = MarketDataBox.ofScenarioValues(15, 16, 17, 18);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> box.combineWith(otherBox, (v1, v2) -> v1 + v2))
        .withMessageStartingWith("Scenario values must have the same number of scenarios");
  }

  @Test
  public void getMarketDataType() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    assertThat(box.getMarketDataType()).isEqualTo(Integer.class);
  }

  @Test
  public void stream() {
    MarketDataBox<Integer> box = MarketDataBox.ofScenarioValues(27, 28, 29);
    List<Integer> list = box.stream().collect(toList());
    assertThat(list).isEqualTo(ImmutableList.of(27, 28, 29));
  }
}
