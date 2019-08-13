/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class LocalDateUtilsTest {

  @Test
  public void test_dayOfYear() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertThat(LocalDateUtils.doy(date)).isEqualTo(date.getDayOfYear());
      date = date.plusDays(1);
    }
  }

  @Test
  public void test_plusDays0() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertThat(LocalDateUtils.plusDays(date, 0)).isEqualTo(date.plusDays(0));
      date = date.plusDays(1);
    }
  }

  @Test
  public void test_plusDays1() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertThat(LocalDateUtils.plusDays(date, 1)).isEqualTo(date.plusDays(1));
      date = date.plusDays(1);
    }
  }

  @Test
  public void test_plusDays3() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertThat(LocalDateUtils.plusDays(date, 3)).isEqualTo(date.plusDays(3));
      date = date.plusDays(1);
    }
  }

  @Test
  public void test_plusDays99() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertThat(LocalDateUtils.plusDays(date, 99)).isEqualTo(date.plusDays(99));
      date = date.plusDays(1);
    }
  }

  @Test
  public void test_plusDaysM1() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertThat(LocalDateUtils.plusDays(date, -1)).isEqualTo(date.plusDays(-1));
      date = date.plusDays(1);
    }
  }

  @Test
  public void test_daysBetween() {
    LocalDate base = LocalDate.of(2012, 1, 1);
    LocalDate date = base;
    for (int i = 0; i < 366 * 8; i++) {
      assertThat(LocalDateUtils.daysBetween(base, date)).isEqualTo(date.toEpochDay() - base.toEpochDay());
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    assertUtilityClass(LocalDateUtils.class);
  }

}
