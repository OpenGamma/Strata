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

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SwaptionExerciseDate}.
 */
public class SwaptionExerciseDateTest {

  private static final LocalDate DATE_05_29 = date(2021, 5, 29);  // Sat
  private static final LocalDate DATE_06_01 = date(2021, 6, 1);  // Tue
  private static final LocalDate DATE_06_30 = date(2021, 6, 30);  // Wed
  private static final LocalDate DATE_08_30 = date(2021, 8, 30);  // Mon holiday
  private static final LocalDate DATE_08_31 = date(2021, 8, 31);  // Tue
  private static final LocalDate DATE_09_01 = date(2021, 9, 1);  // Wed

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionExerciseDate test = SwaptionExerciseDate.of(DATE_08_31, DATE_08_30, DATE_09_01);
    SwaptionExerciseDate test2 = SwaptionExerciseDate.of(DATE_06_01, DATE_05_29, DATE_06_30);
    coverImmutableBean(test);
    coverBeanEquals(test, test2);
    assertSerialization(test);
  }

}
