/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link PositionInfo}.
 */
public class PositionInfoTest {

  private static final StandardId ID = StandardId.of("OG-Test", "123");
  private static final StandardId ID2 = StandardId.of("OG-Test", "321");

  @Test
  public void test_builder() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .build();
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getAttributeTypes()).isEmpty();
    assertThat(test.getAttributes()).isEmpty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getAttribute(AttributeType.DESCRIPTION));
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).isEmpty();
  }

  @Test
  public void test_builder_withers() {
    PositionInfo test = PositionInfo.builder()
        .build()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION);
    assertThat(test.getAttributes()).containsEntry(AttributeType.DESCRIPTION, "A");
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("A");
  }

  @Test
  public void test_builder_with_bulk() {
    Attributes override = Attributes.of(AttributeType.DESCRIPTION, "B").withAttribute(AttributeType.NAME, "C");
    PositionInfo test = PositionInfo.builder()
        .build()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A")
        .withAttributes(override);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("B");
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("C");
  }

  @Test
  public void test_combinedWith() {
    PositionInfo base = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    PositionInfo other = PositionInfo.builder()
        .id(ID2)
        .addAttribute(AttributeType.DESCRIPTION, "B")
        .addAttribute(AttributeType.NAME, "B")
        .build();
    PositionInfo test = base.combinedWith(other);
    assertThat(test.getId()).hasValue(ID);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttributes())
        .containsEntry(AttributeType.DESCRIPTION, "A")
        .containsEntry(AttributeType.NAME, "B");
  }

  @Test
  public void test_overrideWith() {
    PositionInfo base = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    PositionInfo other = PositionInfo.builder()
        .id(ID2)
        .addAttribute(AttributeType.DESCRIPTION, "B")
        .addAttribute(AttributeType.NAME, "B")
        .build();
    PositionInfo test = base.overrideWith(other);
    assertThat(test.getId()).hasValue(ID2);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttributes())
        .containsEntry(AttributeType.DESCRIPTION, "B")
        .containsEntry(AttributeType.NAME, "B");
  }

  @Test
  public void test_overrideWith_otherType() {
    PositionInfo base = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    PortfolioItemInfo other = PortfolioItemInfo.empty()
        .withId(ID2)
        .withAttribute(AttributeType.DESCRIPTION, "B")
        .withAttribute(AttributeType.NAME, "B");
    PositionInfo test = base.overrideWith(other);
    assertThat(test.getId()).hasValue(ID2);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttributes())
        .containsEntry(AttributeType.DESCRIPTION, "B")
        .containsEntry(AttributeType.NAME, "B");
  }

  @Test
  public void test_toBuilder() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .build()
        .toBuilder()
        .id(ID2)
        .build();
    assertThat(test.getId()).hasValue(ID2);
    assertThat(test.getAttributes()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_serialization() {
    PositionInfo test = PositionInfo.builder()
        .id(ID)
        .addAttribute(AttributeType.DESCRIPTION, "A")
        .build();
    assertSerialization(test);
  }

}
