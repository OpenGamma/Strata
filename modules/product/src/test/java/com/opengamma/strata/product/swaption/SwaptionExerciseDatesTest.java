/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link SwaptionExerciseDates}.
 */
public class SwaptionExerciseDatesTest {

  private static final LocalDate DATE_05_29 = date(2021, 5, 29);  // Sat
  private static final LocalDate DATE_06_01 = date(2021, 6, 1);  // Tue
  private static final LocalDate DATE_06_30 = date(2021, 6, 30);  // Wed
  private static final LocalDate DATE_08_30 = date(2021, 8, 30);  // Mon holiday
  private static final LocalDate DATE_08_31 = date(2021, 8, 31);  // Tue
  private static final LocalDate DATE_09_01 = date(2021, 9, 1);  // Wed

  //-------------------------------------------------------------------------
  @Test
  public void test_european() {
    SwaptionExerciseDate date = SwaptionExerciseDate.of(DATE_06_01, DATE_05_29, DATE_06_30);
    SwaptionExerciseDates test = SwaptionExerciseDates.builder()
        .dates(date)
        .allDates(false)
        .build();
    assertThat(test.isEuropean()).isTrue();
    assertThat(test.isAmerican()).isFalse();
    assertThat(test.isBermudan()).isFalse();
  }

  @Test
  public void test_american() {
    SwaptionExerciseDate date1 = SwaptionExerciseDate.of(DATE_06_01, DATE_05_29, DATE_06_30);
    SwaptionExerciseDate date2 = SwaptionExerciseDate.of(DATE_08_31, DATE_08_30, DATE_09_01);
    SwaptionExerciseDates test = SwaptionExerciseDates.builder()
        .dates(date1, date2)
        .allDates(true)
        .build();
    assertThat(test.isEuropean()).isFalse();
    assertThat(test.isAmerican()).isTrue();
    assertThat(test.isBermudan()).isFalse();
  }

  @Test
  public void coverage() {
    SwaptionExerciseDate date1 = SwaptionExerciseDate.of(DATE_06_01, DATE_05_29, DATE_06_30);
    SwaptionExerciseDate date2 = SwaptionExerciseDate.of(DATE_08_31, DATE_08_30, DATE_09_01);
    SwaptionExerciseDates test = SwaptionExerciseDates.of(ImmutableList.of(date1, date2), true);
    SwaptionExerciseDates test2 = SwaptionExerciseDates.ofEuropean(date2);
    coverImmutableBean(test);
    coverBeanEquals(test, test2);
    assertSerialization(test);
  }

}
