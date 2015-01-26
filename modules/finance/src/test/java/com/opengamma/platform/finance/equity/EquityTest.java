/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.equity;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.SecurityType;

/**
 * Test.
 */
@Test
public class EquityTest {

  public void test_of() {
    Equity test = Equity.builder()
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .companyName("OpenGamma")
        .currency(Currency.GBP)
        .build();
    assertEquals(test.getSecurityType(), SecurityType.of("Equity"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Equity test = Equity.builder()
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .companyName("OpenGamma")
        .currency(Currency.GBP)
        .build();
    coverImmutableBean(test);
  }

  public void test_serialization() {
    Equity test = Equity.builder()
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .companyName("OpenGamma")
        .currency(Currency.GBP)
        .build();
    assertSerialization(test);
  }

}
