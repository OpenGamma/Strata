/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link SimpleAttributes}.
 */
public class SimpleAttributesTest {

  @Test
  public void test_empty() {
    SimpleAttributes test = SimpleAttributes.empty();
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).isEmpty();
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.DESCRIPTION));

    SimpleAttributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertThat(test2.getAttribute(AttributeType.NAME)).isEqualTo("world");
  }

  @Test
  public void test_single() {
    SimpleAttributes test = SimpleAttributes.of(AttributeType.DESCRIPTION, "hello");
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("hello");
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");
    assertThat(test.containsAttribute(AttributeType.DESCRIPTION)).isTrue();

    SimpleAttributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertThat(test2.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");
    assertThat(test2.getAttribute(AttributeType.NAME)).isEqualTo("world");
    assertThat(test2.containsAttribute(AttributeType.DESCRIPTION)).isTrue();
  }

  @Test
  public void test_with() {
    SimpleAttributes base = SimpleAttributes.of(AttributeType.DESCRIPTION, "hello");
    assertThat(base.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(base.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");

    SimpleAttributes base2 = SimpleAttributes.of(AttributeType.NAME, "world");
    assertThat(base2.getAttributeTypes()).containsOnly(AttributeType.NAME);
    assertThat(base2.getAttribute(AttributeType.NAME)).isEqualTo("world");

    SimpleAttributes combined = base.withAttributes(base2);
    assertThat(combined.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(combined.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");
    assertThat(combined.getAttribute(AttributeType.NAME)).isEqualTo("world");

    SimpleAttributes selfCombined = base.withAttributes(base);
    assertThat(selfCombined).isEqualTo(base);
  }

  @Test
  public void test_with_remove() {
    SimpleAttributes base = SimpleAttributes.of(AttributeType.DESCRIPTION, "hello");
    assertThat(base.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(base.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");

    SimpleAttributes combined = base.withAttribute(AttributeType.DESCRIPTION, null);
    assertThat(combined.getAttributeTypes()).isEmpty();
  }

  @Test
  public void test_from_SimpleAttributes() {
    SimpleAttributes base = SimpleAttributes.of(AttributeType.DESCRIPTION, "hello");
    assertThat(base.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(base.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");

    SimpleAttributes test = SimpleAttributes.from(base);
    assertThat(test).isEqualTo(base);
  }

  @Test
  public void test_from_OtherType() {
    PositionInfo base = PositionInfo.of(StandardId.of("A", "B")).withAttribute(AttributeType.DESCRIPTION, "hello");
    assertThat(base.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(base.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("hello");

    SimpleAttributes test = SimpleAttributes.from(base);
    assertThat(test).isEqualTo(SimpleAttributes.of(AttributeType.DESCRIPTION, "hello"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableBean test = SimpleAttributes.of(AttributeType.DESCRIPTION, "hello");
    coverImmutableBean(test);
    ImmutableBean test2 = SimpleAttributes.empty();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SimpleAttributes test = SimpleAttributes.of(AttributeType.DESCRIPTION, "hello");
    assertSerialization(test);
  }

}
