/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link PositionInfo}.
 */
@Test
public class PositionInfoTest {

  private static final StandardId ID = StandardId.of("OG-Test", "123");
  private static final StandardId ID2 = StandardId.of("OG-Test", "321");

  public void test_builder() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .build();
    assertEquals(test.getId(), Optional.of(ID));
    assertEquals(test.getAttributeTypes(), ImmutableSet.of());
    assertEquals(test.getAttributes(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.getAttribute(AttributeType.DESCRIPTION));
    assertEquals(test.findAttribute(AttributeType.DESCRIPTION), Optional.empty());
  }

  public void test_builder_withers() {
    PositionInfo test = PositionInfo.builder()
        .build()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertEquals(test.getId(), Optional.of(ID));
    assertEquals(test.getAttributeTypes(), ImmutableSet.of(AttributeType.DESCRIPTION));
    assertEquals(test.getAttributes(), ImmutableMap.of(AttributeType.DESCRIPTION, "A"));
    assertEquals(test.getAttribute(AttributeType.DESCRIPTION), "A");
    assertEquals(test.findAttribute(AttributeType.DESCRIPTION), Optional.of("A"));
  }

  public void test_toBuilder() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .build()
        .toBuilder()
        .id(ID2)
        .build();
    assertEquals(test.getId(), Optional.of(ID2));
    assertEquals(test.getAttributes(), ImmutableMap.of());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    coverImmutableBean(test);
    PositionInfo test2 = PositionInfo.builder()
        .id(ID2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    assertSerialization(test);
  }

}
