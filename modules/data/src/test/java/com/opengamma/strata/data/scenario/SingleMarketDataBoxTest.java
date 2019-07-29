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
 * Test {@link SingleMarketDataBox}.
 */
public class SingleMarketDataBoxTest {

  @Test
  public void isSingleOrScenarioValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    assertThat(box.isSingleValue()).isTrue();
    assertThat(box.isScenarioValue()).isFalse();
  }

  @Test
  public void getSingleValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    assertThat(box.getSingleValue()).isEqualTo(27);
  }

  /**
   * Test that the box always returns the same value for any non-negative scenario index.
   */
  @Test
  public void getValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    assertThat(box.getValue(0)).isEqualTo(27);
    assertThat(box.getValue(Integer.MAX_VALUE)).isEqualTo(27);
    assertThatIllegalArgumentException().isThrownBy(() -> box.getValue(-1));
  }

  @Test
  public void getScenarioValue() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    assertThatIllegalStateException()
        .isThrownBy(box::getScenarioValue)
        .withMessage("This box does not contain a scenario value");
  }

  @Test
  public void getScenarioCount() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    assertThat(box.getScenarioCount()).isEqualTo(-1);
  }

  @Test
  public void map() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    MarketDataBox<Integer> result = box.map(v -> v * 2);
    assertThat(result).isEqualTo(MarketDataBox.ofSingleValue(54));
  }

  /**
   * Tests that applying a function multiple times to the value creates a box of scenario values.
   */
  @Test
  public void mapWithIndex() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    MarketDataBox<Integer> scenarioBox = box.mapWithIndex(3, (v, idx) -> v + idx);
    assertThat(scenarioBox.isScenarioValue()).isTrue();
    assertThat(scenarioBox.getScenarioCount()).isEqualTo(3);
    assertThat(scenarioBox.getValue(0)).isEqualTo(27);
    assertThat(scenarioBox.getValue(1)).isEqualTo(28);
    assertThat(scenarioBox.getValue(2)).isEqualTo(29);
  }

  @Test
  public void combineWithSingleBox() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    MarketDataBox<Integer> otherBox = MarketDataBox.ofSingleValue(15);
    MarketDataBox<Integer> resultBox = box.combineWith(otherBox, (v1, v2) -> v1 + v2);
    assertThat(resultBox.isSingleValue()).isTrue();
    assertThat(resultBox.getValue(0)).isEqualTo(42);
  }

  @Test
  public void combineWithScenarioBox() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    MarketDataBox<Integer> otherBox = MarketDataBox.ofScenarioValues(15, 16, 17);
    MarketDataBox<Integer> resultBox = box.combineWith(otherBox, (v1, v2) -> v1 + v2);
    assertThat(resultBox.isScenarioValue()).isTrue();
    assertThat(resultBox.getScenarioCount()).isEqualTo(3);
    assertThat(resultBox.getValue(0)).isEqualTo(42);
    assertThat(resultBox.getValue(1)).isEqualTo(43);
    assertThat(resultBox.getValue(2)).isEqualTo(44);
  }

  @Test
  public void getMarketDataType() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    assertThat(box.getMarketDataType()).isEqualTo(Integer.class);
  }

  @Test
  public void stream() {
    MarketDataBox<Integer> box = MarketDataBox.ofSingleValue(27);
    List<Integer> list = box.stream().collect(toList());
    assertThat(list).isEqualTo(ImmutableList.of(27));
  }
}
