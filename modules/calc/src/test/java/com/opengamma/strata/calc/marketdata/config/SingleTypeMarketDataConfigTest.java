/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.config;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;

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

    assertThat(configs.get("foo")).hasValue(1);
    assertThat(configs.get("bar")).hasValue(2);
    assertThat(configs.get("baz")).isEmpty();
  }

  public void addValue() {
    Map<String, Object> values = ImmutableMap.of("foo", 1, "bar", 2);
    SingleTypeMarketDataConfig configs = SingleTypeMarketDataConfig.builder()
        .configType(Integer.class)
        .configObjects(values)
        .build()
        .withConfig("baz", 3);

    assertThat(configs.get("foo")).hasValue(1);
    assertThat(configs.get("bar")).hasValue(2);
    assertThat(configs.get("baz")).hasValue(3);
  }

  public void addValueWrongType() {
    assertThrows(
        () -> SingleTypeMarketDataConfig.builder().configType(Integer.class).build().withConfig("baz", "3"),
        IllegalArgumentException.class,
        ".* not of the required type .*");
  }
}
