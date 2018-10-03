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

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link PortfolioItemInfo}.
 */
@Test
public class PortfolioItemInfoTest {

  private static final StandardId ID = StandardId.of("OG-Test", "123");
  private static final StandardId ID2 = StandardId.of("OG-Test", "321");

  public void test_withers() {
    PortfolioItemInfo test = PortfolioItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertEquals(test.getId(), Optional.of(ID));
    assertEquals(test.getAttributeTypes(), ImmutableSet.of(AttributeType.DESCRIPTION));
    assertEquals(test.getAttribute(AttributeType.DESCRIPTION), "A");
    assertEquals(test.findAttribute(AttributeType.DESCRIPTION), Optional.of("A"));
    assertThrows(IllegalArgumentException.class, () -> test.getAttribute(AttributeType.NAME));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ItemInfo test = ItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    coverImmutableBean(test);
    ItemInfo test2 = ItemInfo.empty()
        .withId(ID2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ItemInfo test = ItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertSerialization(test);
  }

}
