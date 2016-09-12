/**
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
    assertEquals(test.getAttributes(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.getAttribute(PositionAttributeType.DESCRIPTION));
    assertEquals(test.findAttribute(PositionAttributeType.DESCRIPTION), Optional.empty());
  }

  public void test_builder_withAttribute() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .build()
        .withAttribute(PositionAttributeType.DESCRIPTION, "A");
    assertEquals(test.getId(), Optional.of(ID));
    assertEquals(test.getAttributes(), ImmutableMap.of(PositionAttributeType.DESCRIPTION, "A"));
    assertEquals(test.getAttribute(PositionAttributeType.DESCRIPTION), "A");
    assertEquals(test.findAttribute(PositionAttributeType.DESCRIPTION), Optional.of("A"));
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
        .addAttribute(PositionAttributeType.DESCRIPTION, "A")
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
        .addAttribute(PositionAttributeType.DESCRIPTION, "A")
        .build();
    assertSerialization(test);
  }

}
