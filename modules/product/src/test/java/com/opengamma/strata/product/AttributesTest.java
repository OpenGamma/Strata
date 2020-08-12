/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link Attributes}.
 */
public class AttributesTest {

  @Test
  public void test_empty() {
    Attributes test = Attributes.empty();
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).isEmpty();
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.DESCRIPTION));

    Attributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertThat(test2.getAttribute(AttributeType.NAME)).isEqualTo("world");
  }

  @Test
  public void test_single() {
    Attributes test = Attributes.of(AttributeType.DESCRIPTION, "hello");
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("hello");
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION)).isTrue();
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION, "hello")).isTrue();
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION, "world")).isFalse();

    Attributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertThat(test2.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");
    assertThat(test2.getAttribute(AttributeType.NAME)).isEqualTo("world");
    assertThat(test2.containsAttribute(AttributeType.DESCRIPTION)).isTrue();
    assertThat(test2.containsAttribute(AttributeType.DESCRIPTION, "hello")).isTrue();
    assertThat(test2.containsAttribute(AttributeType.DESCRIPTION, "world")).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    Attributes test = Attributes.of(AttributeType.DESCRIPTION, "hello");
    assertSerialization(test);
  }

}
