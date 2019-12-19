/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link SecurityInfo}.
 */
public class SecurityInfoTest {

  private static final SecurityId ID = SecurityId.of("OG-Test", "Test");
  private static final SecurityId ID2 = SecurityId.of("OG-Test", "Test2");
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
  private static final SecurityPriceInfo PRICE_INFO2 = SecurityPriceInfo.of(0.02, CurrencyAmount.of(GBP, 1));
  private static final ImmutableMap<AttributeType<?>, Object> INFO_MAP = ImmutableMap.of(AttributeType.NAME, "A");

  //-------------------------------------------------------------------------
  @Test
  public void test_of_priceInfoFields() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO.getTickSize(), PRICE_INFO.getTickValue());
    assertThat(test.getId()).isEqualTo(ID);
    assertThat(test.getPriceInfo()).isEqualTo(PRICE_INFO);
    assertThat(test.getAttributeTypes()).isEmpty();
    assertThat(test.getAttributes()).isEmpty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getAttribute(AttributeType.NAME));
    assertThat(test.findAttribute(AttributeType.NAME)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_priceInfo() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    assertThat(test.getId()).isEqualTo(ID);
    assertThat(test.getPriceInfo()).isEqualTo(PRICE_INFO);
    assertThat(test.getAttributes()).isEmpty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getAttribute(AttributeType.NAME));
    assertThat(test.findAttribute(AttributeType.NAME)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_withAdditionalInfo() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO)
        .withAttribute(AttributeType.NAME, "B")
        .withAttribute(AttributeType.NAME, "A");  // overwrites "B"
    assertThat(test.getId()).isEqualTo(ID);
    assertThat(test.getPriceInfo()).isEqualTo(PRICE_INFO);
    assertThat(test.getAttributes()).isEqualTo(INFO_MAP);
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.NAME)).isEqualTo(Optional.of("A"));
  }

  @Test
  public void test_builder_with_bulk() {
    Attributes override = Attributes.of(AttributeType.DESCRIPTION, "B").withAttribute(AttributeType.NAME, "C");
    SecurityInfo test = SecurityInfo.builder()
        .id(ID)
        .priceInfo(PRICE_INFO)
        .build()
        .withAttribute(AttributeType.DESCRIPTION, "A")
        .withAttributes(override);
    assertThat(test.getId()).isEqualTo(ID);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("B");
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("C");
  }

  @Test
  public void test_builder() {
    SecurityInfo test = SecurityInfo.builder()
        .id(ID)
        .priceInfo(PRICE_INFO)
        .addAttribute(AttributeType.NAME, "B")
        .addAttribute(AttributeType.NAME, "A")  // overwrites "B"
        .build();
    assertThat(test.getId()).isEqualTo(ID);
    assertThat(test.getPriceInfo()).isEqualTo(PRICE_INFO);
    assertThat(test.getAttributes()).isEqualTo(INFO_MAP);
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.NAME)).isEqualTo(Optional.of("A"));
  }

  @Test
  public void test_toBuilder() {
    SecurityInfo test = SecurityInfo.builder()
        .addAttribute(AttributeType.NAME, "name")
        .id(ID)
        .priceInfo(PRICE_INFO)
        .build()
        .toBuilder()
        .id(ID2)
        .build();
    assertThat(test.getId()).isEqualTo(ID2);
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("name");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    coverImmutableBean(test);
    SecurityInfo test2 = SecurityInfo.of(ID2, PRICE_INFO2).withAttribute(AttributeType.NAME, "A");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    assertSerialization(test);
  }

}
