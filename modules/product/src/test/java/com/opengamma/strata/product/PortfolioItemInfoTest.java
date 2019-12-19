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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link PortfolioItemInfo}.
 */
public class PortfolioItemInfoTest {

  private static final StandardId ID = StandardId.of("OG-Test", "123");
  private static final StandardId ID2 = StandardId.of("OG-Test", "321");

  @Test
  public void test_of() {
    PortfolioItemInfo test = PortfolioItemInfo.of(AttributeType.DESCRIPTION, "A");
    assertThat(test.getId()).isEmpty();
    assertThat(test.getAttributeTypes()).isEqualTo(ImmutableSet.of(AttributeType.DESCRIPTION));
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("A");
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.NAME));
  }

  @Test
  public void test_builder() {
    PortfolioItemInfo test = PortfolioItemInfo.builder()
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    assertThat(test.getId()).isEmpty();
    assertThat(test.getAttributeTypes()).isEqualTo(ImmutableSet.of(AttributeType.DESCRIPTION));
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("A");
  }

  @Test
  public void test_withers() {
    PortfolioItemInfo test = PortfolioItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertThat(test.getId()).isEqualTo(Optional.of(ID));
    assertThat(test.getAttributeTypes()).isEqualTo(ImmutableSet.of(AttributeType.DESCRIPTION));
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("A");
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.NAME));
  }

  @Test
  public void test_with_bulk() {
    Attributes override = Attributes.of(AttributeType.DESCRIPTION, "B").withAttribute(AttributeType.NAME, "C");
    PortfolioItemInfo test = PortfolioItemInfo.empty()
        .withAttribute(AttributeType.DESCRIPTION, "A")
        .withAttributes(override);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("B");
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("C");
  }

  @Test
  public void test_combinedWith() {
    PortfolioItemInfo base = PortfolioItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    PositionInfo other = PositionInfo.empty()
        .withId(ID2)
        .withAttribute(AttributeType.DESCRIPTION, "B")
        .withAttribute(AttributeType.NAME, "B");
    PortfolioItemInfo test = base.combinedWith(other);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
  }

  @Test
  public void test_overrideWith() {
    PortfolioItemInfo base = PortfolioItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    PositionInfo other = PositionInfo.empty()
        .withId(ID2)
        .withAttribute(AttributeType.DESCRIPTION, "B")
        .withAttribute(AttributeType.NAME, "B");
    PortfolioItemInfo test = base.overrideWith(other);
    assertThat(test.getId()).hasValue(ID2);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("B");
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("B");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ItemInfo test = ItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    coverImmutableBean(test);
    ItemInfo test2 = ItemInfo.empty()
        .withId(ID2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ItemInfo test = ItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertSerialization(test);
  }

}
