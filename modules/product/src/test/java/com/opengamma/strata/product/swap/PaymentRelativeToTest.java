/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test.
 */
public class PaymentRelativeToTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {PaymentRelativeTo.PERIOD_START, "PeriodStart"},
        {PaymentRelativeTo.PERIOD_END, "PeriodEnd"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(PaymentRelativeTo convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(PaymentRelativeTo convention, String name) {
    assertThat(PaymentRelativeTo.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PaymentRelativeTo.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PaymentRelativeTo.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void selectDate() {
    LocalDate date1 = date(2014, 3, 27);
    LocalDate date2 = date(2014, 6, 27);
    SchedulePeriod period = SchedulePeriod.of(date1, date2);
    assertThat(PaymentRelativeTo.PERIOD_START.selectBaseDate(period)).isEqualTo(date1);
    assertThat(PaymentRelativeTo.PERIOD_END.selectBaseDate(period)).isEqualTo(date2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(PaymentRelativeTo.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(PaymentRelativeTo.PERIOD_START);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(PaymentRelativeTo.class, PaymentRelativeTo.PERIOD_START);
  }

}
