/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

/**
 * Test {@link Attributes}.
 */
@Test
public class AttributesTest {

  public void test_empty() {
    Attributes test = Attributes.empty();
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).isEmpty();
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.DESCRIPTION));

    Attributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertThat(test2.getAttribute(AttributeType.NAME)).isEqualTo("world");
  }

  public void test_single() {
    Attributes test = Attributes.of(AttributeType.DESCRIPTION, "hello");
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("hello");
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION)).isTrue();

    Attributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertThat(test2.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");
    assertThat(test2.getAttribute(AttributeType.NAME)).isEqualTo("world");
    assertThat(test2.containsAttribute(AttributeType.DESCRIPTION)).isTrue();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableBean test = (ImmutableBean) Attributes.of(AttributeType.DESCRIPTION, "hello");
    coverImmutableBean(test);
    ImmutableBean test2 = (ImmutableBean) Attributes.empty();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    Attributes test = Attributes.of(AttributeType.DESCRIPTION, "hello");
    assertSerialization(test);
  }

}
