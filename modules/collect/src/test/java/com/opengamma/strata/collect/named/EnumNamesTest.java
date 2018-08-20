/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link EnumNames}.
 */
@Test
public class EnumNamesTest {

  public void test_format() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertEquals(test.format(MockEnum.ONE), "One");
    assertEquals(test.format(MockEnum.TWENTY_ONE), "TwentyOne");
    assertEquals(test.format(MockEnum.FooBar), "Foobar");
    assertEquals(test.format(MockEnum.WOO_BAR_WAA), "WooBarWaa");
  }

  public void test_parse_one() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertEquals(test.parse("One"), MockEnum.ONE);
    assertEquals(test.parse("ONE"), MockEnum.ONE);
    assertEquals(test.parse("one"), MockEnum.ONE);
  }

  public void test_parse_twentyOne() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertEquals(test.parse("TwentyOne"), MockEnum.TWENTY_ONE);
    assertEquals(test.parse("TWENTYONE"), MockEnum.TWENTY_ONE);
    assertEquals(test.parse("twentyone"), MockEnum.TWENTY_ONE);
    assertEquals(test.parse("TWENTY_ONE"), MockEnum.TWENTY_ONE);
    assertEquals(test.parse("twenty_one"), MockEnum.TWENTY_ONE);
  }

  public void test_parse_fooBar() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertEquals(test.parse("Foobar"), MockEnum.FooBar);
    assertEquals(test.parse("FOOBAR"), MockEnum.FooBar);
    assertEquals(test.parse("foobar"), MockEnum.FooBar);
    assertEquals(test.parse("FooBar"), MockEnum.FooBar);
  }

  public void test_parse_wooBarWaa() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertEquals(test.parse("WooBarWaa"), MockEnum.WOO_BAR_WAA);
    assertEquals(test.parse("WOOBARWAA"), MockEnum.WOO_BAR_WAA);
    assertEquals(test.parse("woobarwaa"), MockEnum.WOO_BAR_WAA);
    assertEquals(test.parse("WOO_BAR_WAA"), MockEnum.WOO_BAR_WAA);
    assertEquals(test.parse("woo_bar_waa"), MockEnum.WOO_BAR_WAA);
  }

  public void test_parse_invalid() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertThatThrownBy(() -> test.parse("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown enum name 'unknown' for type " + MockEnum.class.getName());
  }

  static enum MockEnum implements NamedEnum {
    ONE,
    TWENTY_ONE,
    FooBar,
    WOO_BAR_WAA,
  }

}
