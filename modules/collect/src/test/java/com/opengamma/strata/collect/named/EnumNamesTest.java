/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Test {@link EnumNames}.
 */
public class EnumNamesTest {

  @Test
  public void test_format() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertThat(test.format(MockEnum.ONE)).isEqualTo("One");
    assertThat(test.format(MockEnum.TWENTY_ONE)).isEqualTo("TwentyOne");
    assertThat(test.format(MockEnum.FooBar)).isEqualTo("Foobar");
    assertThat(test.format(MockEnum.WOO_BAR_WAA)).isEqualTo("WooBarWaa");
  }

  @Test
  public void test_parse_one() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertThat(test.parse("One")).isEqualTo(MockEnum.ONE);
    assertThat(test.parse("ONE")).isEqualTo(MockEnum.ONE);
    assertThat(test.parse("one")).isEqualTo(MockEnum.ONE);
  }

  @Test
  public void test_parse_twentyOne() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertThat(test.parse("TwentyOne")).isEqualTo(MockEnum.TWENTY_ONE);
    assertThat(test.parse("TWENTYONE")).isEqualTo(MockEnum.TWENTY_ONE);
    assertThat(test.parse("twentyone")).isEqualTo(MockEnum.TWENTY_ONE);
    assertThat(test.parse("TWENTY_ONE")).isEqualTo(MockEnum.TWENTY_ONE);
    assertThat(test.parse("twenty_one")).isEqualTo(MockEnum.TWENTY_ONE);
  }

  @Test
  public void test_parse_fooBar() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertThat(test.parse("Foobar")).isEqualTo(MockEnum.FooBar);
    assertThat(test.parse("FOOBAR")).isEqualTo(MockEnum.FooBar);
    assertThat(test.parse("foobar")).isEqualTo(MockEnum.FooBar);
    assertThat(test.parse("FooBar")).isEqualTo(MockEnum.FooBar);
  }

  @Test
  public void test_parse_wooBarWaa() {
    EnumNames<MockEnum> test = EnumNames.of(MockEnum.class);
    assertThat(test.parse("WooBarWaa")).isEqualTo(MockEnum.WOO_BAR_WAA);
    assertThat(test.parse("WOOBARWAA")).isEqualTo(MockEnum.WOO_BAR_WAA);
    assertThat(test.parse("woobarwaa")).isEqualTo(MockEnum.WOO_BAR_WAA);
    assertThat(test.parse("WOO_BAR_WAA")).isEqualTo(MockEnum.WOO_BAR_WAA);
    assertThat(test.parse("woo_bar_waa")).isEqualTo(MockEnum.WOO_BAR_WAA);
  }

  @Test
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
