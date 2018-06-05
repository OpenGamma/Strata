/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

/**
 * Test {@link Attributes}.
 */
@Test
public class AttributesTest {

  public void test_empty() {
    Attributes test = Attributes.empty();
    assertEquals(test.findAttribute(AttributeType.DESCRIPTION), Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> test.getAttribute(AttributeType.DESCRIPTION));

    Attributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertEquals(test2.getAttribute(AttributeType.NAME), "world");
  }

  public void test_single() {
    Attributes test = Attributes.of(AttributeType.DESCRIPTION, "hello");
    assertEquals(test.findAttribute(AttributeType.DESCRIPTION), Optional.of("hello"));
    assertEquals(test.getAttribute(AttributeType.DESCRIPTION), "hello");

    Attributes test2 = test.withAttribute(AttributeType.NAME, "world");
    assertEquals(test2.getAttribute(AttributeType.DESCRIPTION), "hello");
    assertEquals(test2.getAttribute(AttributeType.NAME), "world");
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
