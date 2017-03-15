/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link SecurityInfo}.
 */
@Test
public class SecurityInfoTest {

  private static final SecurityId ID = SecurityId.of("OG-Test", "Test");
  private static final SecurityId ID2 = SecurityId.of("OG-Test", "Test2");
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
  private static final SecurityPriceInfo PRICE_INFO2 = SecurityPriceInfo.of(0.02, CurrencyAmount.of(GBP, 1));
  private static final ImmutableMap<SecurityAttributeType<?>, Object> INFO_MAP = ImmutableMap.of(SecurityAttributeType.NAME, "A");

  //-------------------------------------------------------------------------
  public void test_of_priceInfoFields() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO.getTickSize(), PRICE_INFO.getTickValue());
    assertEquals(test.getId(), ID);
    assertEquals(test.getPriceInfo(), PRICE_INFO);
    assertEquals(test.getAttributes(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.getAttribute(SecurityAttributeType.NAME));
    assertEquals(test.findAttribute(SecurityAttributeType.NAME), Optional.empty());
  }

  public void test_of_priceInfo() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    assertEquals(test.getId(), ID);
    assertEquals(test.getPriceInfo(), PRICE_INFO);
    assertEquals(test.getAttributes(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.getAttribute(SecurityAttributeType.NAME));
    assertEquals(test.findAttribute(SecurityAttributeType.NAME), Optional.empty());
  }

  public void test_of_withAdditionalInfo() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO)
        .withAttribute(SecurityAttributeType.NAME, "B")
        .withAttribute(SecurityAttributeType.NAME, "A");  // overwrites "B"
    assertEquals(test.getId(), ID);
    assertEquals(test.getPriceInfo(), PRICE_INFO);
    assertEquals(test.getAttributes(), INFO_MAP);
    assertEquals(test.getAttribute(SecurityAttributeType.NAME), "A");
    assertEquals(test.findAttribute(SecurityAttributeType.NAME), Optional.of("A"));
  }

  public void test_builder() {
    SecurityInfo test = SecurityInfo.builder()
        .id(ID)
        .priceInfo(PRICE_INFO)
        .addAttribute(SecurityAttributeType.NAME, "B")
        .addAttribute(SecurityAttributeType.NAME, "A")  // overwrites "B"
        .build();
    assertEquals(test.getId(), ID);
    assertEquals(test.getPriceInfo(), PRICE_INFO);
    assertEquals(test.getAttributes(), INFO_MAP);
    assertEquals(test.getAttribute(SecurityAttributeType.NAME), "A");
    assertEquals(test.findAttribute(SecurityAttributeType.NAME), Optional.of("A"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    coverImmutableBean(test);
    SecurityInfo test2 = SecurityInfo.of(ID2, PRICE_INFO2).withAttribute(SecurityAttributeType.NAME, "A");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    assertSerialization(test);
  }

}
