/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

public class SingleTypeMarketDataConfigTest {

  @Test
  public void getValues() {
    Map<String, Object> values = ImmutableMap.of("foo", 1, "bar", 2);
    SingleTypeMarketDataConfig configs = SingleTypeMarketDataConfig.builder()
        .configType(Integer.class)
        .configObjects(values)
        .build();

    assertThat(configs.get("foo")).isEqualTo(1);
    assertThat(configs.get("bar")).isEqualTo(2);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> configs.get("baz"))
        .withMessage("No configuration found with type java.lang.Integer and name baz");
  }

  @Test
  public void addValue() {
    Map<String, Object> values = ImmutableMap.of("foo", 1, "bar", 2);
    SingleTypeMarketDataConfig configs = SingleTypeMarketDataConfig.builder()
        .configType(Integer.class)
        .configObjects(values)
        .build()
        .withConfig("baz", 3);

    assertThat(configs.get("foo")).isEqualTo(1);
    assertThat(configs.get("bar")).isEqualTo(2);
    assertThat(configs.get("baz")).isEqualTo(3);
  }

  @Test
  public void addValueWrongType() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SingleTypeMarketDataConfig.builder().configType(Integer.class).build().withConfig("baz", "3"))
        .withMessageMatching(".* not of the required type .*");
  }
}
