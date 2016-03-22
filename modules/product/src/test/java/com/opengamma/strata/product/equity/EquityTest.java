/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.equity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.SecurityId;

/**
 * Test {@link Equity}.
 */
@Test
public class EquityTest {

  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Equity");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "Equity2");

  //-------------------------------------------------------------------------
  public void test_builder() {
    Equity test = sut();
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getCurrency(), GBP);
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
  static Equity sut() {
    return Equity.builder()
        .securityId(SECURITY_ID)
        .currency(GBP)
        .build();
  }

  static Equity sut2() {
    return Equity.builder()
        .securityId(SECURITY_ID2)
        .currency(USD)
        .build();
  }

}
