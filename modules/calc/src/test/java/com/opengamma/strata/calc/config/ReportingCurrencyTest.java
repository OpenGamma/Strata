/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

/**
 * Test {@link ReportingCurrency}.
 */
@Test
public class ReportingCurrencyTest {

  public void test_NATURAL() {
    ReportingCurrency test = ReportingCurrency.NATURAL;
    assertEquals(test.getType(), ReportingCurrencyType.NATURAL);
    assertEquals(test.getCurrency(), Optional.empty());
  }

  public void test_of() {
    ReportingCurrency test = ReportingCurrency.of(USD);
    assertEquals(test.getType(), ReportingCurrencyType.SPECIFIC);
    assertEquals(test.getCurrency(), Optional.of(USD));
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
