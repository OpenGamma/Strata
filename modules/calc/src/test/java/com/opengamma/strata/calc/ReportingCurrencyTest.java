/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ReportingCurrency}.
 */
public class ReportingCurrencyTest {

  @Test
  public void test_NATURAL() {
    ReportingCurrency test = ReportingCurrency.NATURAL;
    assertThat(test.getType()).isEqualTo(ReportingCurrencyType.NATURAL);
    assertThat(test.isSpecific()).isFalse();
    assertThat(test.isNatural()).isTrue();
    assertThat(test.isNone()).isFalse();
    assertThat(test.toString()).isEqualTo("Natural");
    assertThatIllegalStateException().isThrownBy(() -> test.getCurrency());
  }

  @Test
  public void test_NONE() {
    ReportingCurrency test = ReportingCurrency.NONE;
    assertThat(test.getType()).isEqualTo(ReportingCurrencyType.NONE);
    assertThat(test.isSpecific()).isFalse();
    assertThat(test.isNatural()).isFalse();
    assertThat(test.isNone()).isTrue();
    assertThat(test.toString()).isEqualTo("None");
    assertThatIllegalStateException().isThrownBy(() -> test.getCurrency());
  }

  @Test
  public void test_of_specific() {
    ReportingCurrency test = ReportingCurrency.of(USD);
    assertThat(test.getType()).isEqualTo(ReportingCurrencyType.SPECIFIC);
    assertThat(test.isSpecific()).isTrue();
    assertThat(test.isNatural()).isFalse();
    assertThat(test.isNone()).isFalse();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.toString()).isEqualTo("Specific:USD");
  }

  @Test
  public void test_type() {
    assertThat(ReportingCurrencyType.of("Specific").toString()).isEqualTo("Specific");
    assertThat(ReportingCurrencyType.of("Natural").toString()).isEqualTo("Natural");
    assertThat(ReportingCurrencyType.of("None").toString()).isEqualTo("None");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ReportingCurrency test = ReportingCurrency.NATURAL;
    coverImmutableBean(test);
    ReportingCurrency test2 = ReportingCurrency.of(USD);
    coverBeanEquals(test, test2);
    coverEnum(ReportingCurrencyType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(ReportingCurrency.NATURAL);
    assertSerialization(ReportingCurrency.of(USD));
  }

}
