/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link AdjustableDate}.
 */
public class AdjustableDateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.SAT_SUN);

  private static final LocalDate THU_2014_07_10 = LocalDate.of(2014, 7, 10);
  private static final LocalDate FRI_2014_07_11 = LocalDate.of(2014, 7, 11);
  private static final LocalDate SAT_2014_07_12 = LocalDate.of(2014, 7, 12);
  private static final LocalDate SUN_2014_07_13 = LocalDate.of(2014, 7, 13);
  private static final LocalDate MON_2014_07_14 = LocalDate.of(2014, 7, 14);
  private static final LocalDate TUE_2014_07_15 = LocalDate.of(2014, 7, 15);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_1arg() {
    AdjustableDate test = AdjustableDate.of(FRI_2014_07_11);
    assertThat(test.getUnadjusted()).isEqualTo(FRI_2014_07_11);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("2014-07-11");
    assertThat(test.adjusted(REF_DATA)).isEqualTo(FRI_2014_07_11);
  }

  @Test
  public void test_of_2args_withAdjustment() {
    AdjustableDate test = AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN);
    assertThat(test.getUnadjusted()).isEqualTo(FRI_2014_07_11);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("2014-07-11 adjusted by Following using calendar Sat/Sun");
    assertThat(test.adjusted(REF_DATA)).isEqualTo(FRI_2014_07_11);
  }

  @Test
  public void test_of_2args_withNoAdjustment() {
    AdjustableDate test = AdjustableDate.of(FRI_2014_07_11, BDA_NONE);
    assertThat(test.getUnadjusted()).isEqualTo(FRI_2014_07_11);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("2014-07-11");
    assertThat(test.adjusted(REF_DATA)).isEqualTo(FRI_2014_07_11);
  }

  @Test
  public void test_of_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> AdjustableDate.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> AdjustableDate.of(null, BDA_FOLLOW_SAT_SUN));
    assertThatIllegalArgumentException().isThrownBy(() -> AdjustableDate.of(FRI_2014_07_11, null));
    assertThatIllegalArgumentException().isThrownBy(() -> AdjustableDate.of(null, null));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_adjusted() {
    return new Object[][] {
        {THU_2014_07_10, THU_2014_07_10},
        {FRI_2014_07_11, FRI_2014_07_11},
        {SAT_2014_07_12, MON_2014_07_14},
        {SUN_2014_07_13, MON_2014_07_14},
        {MON_2014_07_14, MON_2014_07_14},
        {TUE_2014_07_15, TUE_2014_07_15},
    };
  }

  @ParameterizedTest
  @MethodSource("data_adjusted")
  public void test_adjusted(LocalDate date, LocalDate expected) {
    AdjustableDate test = AdjustableDate.of(date, BDA_FOLLOW_SAT_SUN);
    assertThat(test.adjusted(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void equals() {
    AdjustableDate a1 = AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN);
    AdjustableDate a2 = AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN);
    AdjustableDate b = AdjustableDate.of(SAT_2014_07_12, BDA_FOLLOW_SAT_SUN);
    AdjustableDate c = AdjustableDate.of(FRI_2014_07_11, BDA_NONE);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN));
  }

  @Test
  public void test_serialization() {
    assertSerialization(AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN));
  }

}
