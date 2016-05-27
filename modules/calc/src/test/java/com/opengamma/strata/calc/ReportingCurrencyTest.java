/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ReportingCurrency}.
 */
@Test
public class ReportingCurrencyTest {

  public void test_NATURAL() {
    ReportingCurrency test = ReportingCurrency.NATURAL;
    assertEquals(test.getType(), ReportingCurrencyType.NATURAL);
    assertEquals(test.isNatural(), true);
    assertEquals(test.isSpecific(), false);
    assertEquals(test.toString(), "Natural");
    assertThrows(() -> test.getCurrency(), IllegalStateException.class);
  }

  public void test_of_specific() {
    ReportingCurrency test = ReportingCurrency.of(USD);
    assertEquals(test.getType(), ReportingCurrencyType.SPECIFIC);
    assertEquals(test.isNatural(), false);
    assertEquals(test.isSpecific(), true);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.toString(), "Specific:USD");
  }

  public void test_type() {
    assertEquals(ReportingCurrencyType.of("Specific").toString(), "Specific");
    assertEquals(ReportingCurrencyType.of("Natural").toString(), "Natural");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ReportingCurrency test = ReportingCurrency.NATURAL;
    coverImmutableBean(test);
    ReportingCurrency test2 = ReportingCurrency.of(USD);
    coverBeanEquals(test, test2);
    coverEnum(ReportingCurrencyType.class);
  }

  public void test_serialization() {
    assertSerialization(ReportingCurrency.NATURAL);
    assertSerialization(ReportingCurrency.of(USD));
  }

}
