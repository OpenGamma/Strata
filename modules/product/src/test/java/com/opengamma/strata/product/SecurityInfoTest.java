/**
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
  private static final ImmutableMap<SecurityInfoType<?>, Object> INFO_MAP = ImmutableMap.of(SecurityInfoType.NAME, "A");

  //-------------------------------------------------------------------------
  public void test_of() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    assertEquals(test.getId(), ID);
    assertEquals(test.getPriceInfo(), PRICE_INFO);
    assertEquals(test.getAdditionalInfo(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.getAdditionalInfo(SecurityInfoType.NAME));
    assertEquals(test.findAdditionalInfo(SecurityInfoType.NAME), Optional.empty());
  }

  public void test_of_withAdditionalInfo() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO)
        .withAdditionalInfo(SecurityInfoType.NAME, "B")
        .withAdditionalInfo(SecurityInfoType.NAME, "A");  // overwrites "B"
    assertEquals(test.getId(), ID);
    assertEquals(test.getPriceInfo(), PRICE_INFO);
    assertEquals(test.getAdditionalInfo(), INFO_MAP);
    assertEquals(test.getAdditionalInfo(SecurityInfoType.NAME), "A");
    assertEquals(test.findAdditionalInfo(SecurityInfoType.NAME), Optional.of("A"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    coverImmutableBean(test);
    SecurityInfo test2 = SecurityInfo.of(ID2, PRICE_INFO2).withAdditionalInfo(SecurityInfoType.NAME, "A");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SecurityInfo test = SecurityInfo.of(ID, PRICE_INFO);
    assertSerialization(test);
  }

}
