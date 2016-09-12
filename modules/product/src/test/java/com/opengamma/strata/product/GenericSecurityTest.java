/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link GenericSecurity}.
 */
@Test
public class GenericSecurityTest {

  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(SecurityId.of("Test", "1"), PRICE_INFO);
  private static final SecurityInfo INFO2 = SecurityInfo.of(SecurityId.of("Test", "2"), PRICE_INFO);

  //-------------------------------------------------------------------------
  public void test_of() {
    GenericSecurity test = sut();
    assertEquals(test.getInfo(), INFO);
    assertEquals(test.getSecurityId(), INFO.getId());
    assertEquals(test.getCurrency(), INFO.getPriceInfo().getCurrency());
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of());
    assertEquals(test, GenericSecurity.of(INFO));
    assertThrows(() -> test.createProduct(ReferenceData.empty()), UnsupportedOperationException.class);
    assertEquals(
        test.createTrade(TradeInfo.empty(), 1, 2, ReferenceData.empty()),
        GenericSecurityTrade.of(TradeInfo.empty(), GenericSecurity.of(INFO), 1, 2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static GenericSecurity sut() {
    return GenericSecurity.of(INFO);
  }

  static GenericSecurity sut2() {
    return GenericSecurity.of(INFO2);
  }

}
