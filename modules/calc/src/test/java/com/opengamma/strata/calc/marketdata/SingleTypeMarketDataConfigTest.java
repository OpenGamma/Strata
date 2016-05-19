/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

@Test
public class SingleTypeMarketDataConfigTest {

  public void getValues() {
    Map<String, Object> values = ImmutableMap.of("foo", 1, "bar", 2);
    SingleTypeMarketDataConfig configs = SingleTypeMarketDataConfig.builder()
        .configType(Integer.class)
        .configObjects(values)
        .build();

    assertThat(configs.get("foo")).isEqualTo(1);
    assertThat(configs.get("bar")).isEqualTo(2);
    assertThrowsIllegalArg(() -> configs.get("baz"), "No configuration found with type java.lang.Integer and name baz");
  }

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

  public void addValueWrongType() {
    assertThrowsIllegalArg(
        () -> SingleTypeMarketDataConfig.builder().configType(Integer.class).build().withConfig("baz", "3"),
        ".* not of the required type .*");
  }
}
