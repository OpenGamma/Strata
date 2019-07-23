/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link BusinessDayAdjustment}.
 */
public class BusinessDayAdjustmentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @Test
  public void test_basics() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
    assertThat(test.getConvention()).isEqualTo(MODIFIED_FOLLOWING);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
    assertThat(test.toString()).isEqualTo("ModifiedFollowing using calendar Sat/Sun");
  }

  @ParameterizedTest
  @MethodSource("com.opengamma.strata.basics.date.BusinessDayConventionTest#data_convention")
  public void test_adjustDate(BusinessDayConvention convention, LocalDate input, LocalDate expected) {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(convention, SAT_SUN);
    assertThat(test.adjust(input, REF_DATA)).isEqualTo(expected);
    assertThat(test.resolve(REF_DATA).adjust(input)).isEqualTo(expected);
  }

  @Test
  public void test_noAdjust_constant() {
    BusinessDayAdjustment test = BusinessDayAdjustment.NONE;
    assertThat(test.getConvention()).isEqualTo(BusinessDayConventions.NO_ADJUST);
    assertThat(test.getCalendar()).isEqualTo(NO_HOLIDAYS);
    assertThat(test.toString()).isEqualTo("NoAdjust");
  }

  @Test
  public void test_noAdjust_factory() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(BusinessDayConventions.NO_ADJUST, NO_HOLIDAYS);
    assertThat(test.getConvention()).isEqualTo(BusinessDayConventions.NO_ADJUST);
    assertThat(test.getCalendar()).isEqualTo(NO_HOLIDAYS);
    assertThat(test.toString()).isEqualTo("NoAdjust");
  }

  @Test
  public void test_noAdjust_normalized() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(BusinessDayConventions.NO_ADJUST, SAT_SUN);
    assertThat(test.getConvention()).isEqualTo(BusinessDayConventions.NO_ADJUST);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
    assertThat(test.toString()).isEqualTo("NoAdjust using calendar Sat/Sun");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
  }

  @Test
  public void coverage_builder() {
    BusinessDayAdjustment test = BusinessDayAdjustment.builder()
        .convention(MODIFIED_FOLLOWING)
        .calendar(SAT_SUN)
        .build();
    assertThat(test.getConvention()).isEqualTo(MODIFIED_FOLLOWING);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
  }

  @Test
  public void test_serialization() {
    assertSerialization(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
  }

}
