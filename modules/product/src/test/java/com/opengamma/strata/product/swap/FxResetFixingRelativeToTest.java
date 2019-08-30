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
public class FxResetFixingRelativeToTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FxResetFixingRelativeTo.PERIOD_START, "PeriodStart"},
        {FxResetFixingRelativeTo.PERIOD_END, "PeriodEnd"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FxResetFixingRelativeTo convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FxResetFixingRelativeTo convention, String name) {
    assertThat(FxResetFixingRelativeTo.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxResetFixingRelativeTo.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxResetFixingRelativeTo.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void selectDate() {
    LocalDate date1 = date(2014, 3, 27);
    LocalDate date2 = date(2014, 6, 27);
    SchedulePeriod period = SchedulePeriod.of(date1, date2);
    assertThat(FxResetFixingRelativeTo.PERIOD_START.selectBaseDate(period)).isEqualTo(date1);
    assertThat(FxResetFixingRelativeTo.PERIOD_END.selectBaseDate(period)).isEqualTo(date2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FxResetFixingRelativeTo.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FxResetFixingRelativeTo.PERIOD_START);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FxResetFixingRelativeTo.class, FxResetFixingRelativeTo.PERIOD_START);
  }

}
