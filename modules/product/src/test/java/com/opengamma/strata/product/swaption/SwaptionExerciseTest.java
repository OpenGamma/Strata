/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.AdjustableDates;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link SwaptionExercise}.
 */
public class SwaptionExerciseTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_GBLO = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final LocalDate DATE_05_29 = date(2021, 5, 29);  // Sat
  private static final LocalDate DATE_05_30 = date(2021, 5, 30);  // Sun
  private static final LocalDate DATE_05_31 = date(2021, 5, 31);  // Mon holiday
  private static final LocalDate DATE_06_01 = date(2021, 6, 1);  // Tue
  private static final LocalDate DATE_06_30 = date(2021, 6, 30);  // Wed
  private static final LocalDate DATE_07_30 = date(2021, 7, 30);  // Fri
  private static final LocalDate DATE_08_30 = date(2021, 8, 30);  // Mon holiday
  private static final LocalDate DATE_08_31 = date(2021, 8, 31);  // Tue
  private static final LocalDate DATE_09_01 = date(2021, 9, 1);  // Wed
  private static final AdjustableDate ADJDATE_08_30 = AdjustableDate.of(DATE_08_30, BDA_GBLO);
  private static final DaysAdjustment OFFSET = DaysAdjustment.ofBusinessDays(2, GBLO);

  //-------------------------------------------------------------------------
  @Test
  public void test_ofEuropean() {
    SwaptionExercise test = SwaptionExercise.ofEuropean(ADJDATE_08_30, OFFSET);
    assertThat(test.getDateDefinition()).isEqualTo(AdjustableDates.of(BDA_GBLO, DATE_08_30));
    assertThat(test.getFrequency()).isEmpty();
    assertThat(test.getSwapStartDateOffset()).isEqualTo(OFFSET);
    assertThat(test.isEuropean()).isTrue();
    assertThat(test.isAmerican()).isFalse();
    assertThat(test.isBermudan()).isFalse();
    assertThat(test.calculateDates()).isEqualTo(AdjustableDates.of(BDA_GBLO, DATE_08_30));
    assertThat(test.selectDate(DATE_08_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_08_31, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_31, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> test.selectDate(DATE_06_30, REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofAmerican() {
    SwaptionExercise test = SwaptionExercise.ofAmerican(DATE_05_30, DATE_08_30, BDA_GBLO, OFFSET);
    assertThat(test.getDateDefinition()).isEqualTo(AdjustableDates.of(BDA_GBLO, DATE_05_30, DATE_08_30));
    assertThat(test.getFrequency()).hasValue(Frequency.P1D);
    assertThat(test.getSwapStartDateOffset()).isEqualTo(OFFSET);
    assertThat(test.isEuropean()).isFalse();
    assertThat(test.isAmerican()).isTrue();
    assertThat(test.isBermudan()).isFalse();
    assertThat(test.calculateDates().getUnadjusted()).contains(DATE_05_30, DATE_06_01, DATE_06_30, DATE_08_30);
    assertThat(test.calculateDates().getUnadjusted()).doesNotContain(DATE_05_29, DATE_08_31);
    assertThat(test.calculateDates().getAdjustment()).isEqualTo(BDA_GBLO);
    assertThatIllegalArgumentException().isThrownBy(() -> test.selectDate(DATE_05_29, REF_DATA));
    assertThat(test.selectDate(DATE_05_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_05_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_05_31, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_05_31, BDA_GBLO));
    assertThat(test.selectDate(DATE_06_01, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_06_01, BDA_GBLO));
    assertThat(test.selectDate(DATE_06_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_06_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_07_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_07_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_08_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_08_31, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_31, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> test.selectDate(DATE_09_01, REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofBermudan_explicit() {
    AdjustableDates adjDates = AdjustableDates.of(BDA_GBLO, DATE_06_30, DATE_07_30, DATE_08_30);
    SwaptionExercise test = SwaptionExercise.ofBermudan(adjDates, OFFSET);
    assertThat(test.getDateDefinition()).isEqualTo(adjDates);
    assertThat(test.getFrequency()).isEmpty();
    assertThat(test.getSwapStartDateOffset()).isEqualTo(OFFSET);
    assertThat(test.isEuropean()).isFalse();
    assertThat(test.isAmerican()).isFalse();
    assertThat(test.isBermudan()).isTrue();
    assertThat(test.calculateDates())
        .isEqualTo(AdjustableDates.of(BDA_GBLO, DATE_06_30, DATE_07_30, DATE_08_30));
    assertThatIllegalArgumentException().isThrownBy(() -> test.selectDate(DATE_05_31, REF_DATA));
    assertThat(test.selectDate(DATE_06_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_06_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_07_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_07_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_08_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_08_31, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_31, BDA_NONE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofBermudan_frequency() {
    SwaptionExercise test = SwaptionExercise.ofBermudan(DATE_05_30, DATE_08_30, BDA_GBLO, Frequency.P1M, OFFSET);
    assertThat(test.getDateDefinition()).isEqualTo(AdjustableDates.of(BDA_GBLO, DATE_05_30, DATE_08_30));
    assertThat(test.getFrequency()).hasValue(Frequency.P1M);
    assertThat(test.getSwapStartDateOffset()).isEqualTo(OFFSET);
    assertThat(test.isEuropean()).isFalse();
    assertThat(test.isAmerican()).isFalse();
    assertThat(test.isBermudan()).isTrue();
    assertThat(test.calculateDates())
        .isEqualTo(AdjustableDates.of(BDA_GBLO, DATE_05_30, DATE_06_30, DATE_07_30, DATE_08_30));
    assertThat(test.selectDate(DATE_05_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_05_30, BDA_GBLO));
    assertThatIllegalArgumentException().isThrownBy(() -> test.selectDate(DATE_05_31, REF_DATA));
    assertThat(test.selectDate(DATE_06_01, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_06_01, BDA_NONE));
    assertThat(test.selectDate(DATE_06_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_06_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_07_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_07_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_08_30, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_30, BDA_GBLO));
    assertThat(test.selectDate(DATE_08_31, REF_DATA)).isEqualTo(AdjustableDate.of(DATE_08_31, BDA_NONE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionExercise test = SwaptionExercise.ofEuropean(ADJDATE_08_30, OFFSET);
    SwaptionExercise test2 = SwaptionExercise.ofAmerican(DATE_06_30, DATE_08_30, BDA_GBLO, OFFSET);
    coverImmutableBean(test);
    coverBeanEquals(test, test2);
    assertSerialization(test);
  }

}
