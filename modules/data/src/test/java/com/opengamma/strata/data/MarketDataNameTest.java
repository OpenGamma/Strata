/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link MarketDataName}.
 */
public class MarketDataNameTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of() {
    TestingName test = new TestingName("Foo");
    assertThat(test.getName()).isEqualTo("Foo");
    assertThat(test.getMarketDataType()).isEqualTo(String.class);
    assertThat(test.toString()).isEqualTo("Foo");
  }

  @Test
  public void test_comparison() {
    TestingName test = new TestingName("Foo");
    assertThat(test.equals(test)).isEqualTo(true);
    assertThat(test.hashCode()).isEqualTo(test.hashCode());
    assertThat(test.equals(new TestingName("Eoo"))).isEqualTo(false);
    assertThat(test.equals(new TestingName("Foo"))).isEqualTo(true);
    assertThat(test.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(test.equals(null)).isEqualTo(false);
    assertThat(test.compareTo(new TestingName("Eoo")) > 0).isEqualTo(true);
    assertThat(test.compareTo(new TestingName("Foo")) == 0).isEqualTo(true);
    assertThat(test.compareTo(new TestingName("Goo")) < 0).isEqualTo(true);
    assertThat(test.compareTo(new TestingName2("Foo")) < 0).isEqualTo(true);
  }

}
