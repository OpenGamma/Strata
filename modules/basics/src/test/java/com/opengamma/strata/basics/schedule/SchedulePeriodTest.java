/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_18;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.Month.AUGUST;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.SEPTEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.time.Period;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;

/**
 * Test {@link SchedulePeriod}.
 */
public class SchedulePeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate JUN_15 = date(2014, JUNE, 15);  // Sunday
  private static final LocalDate JUN_16 = date(2014, JUNE, 16);
  private static final LocalDate JUN_17 = date(2014, JUNE, 17);
  private static final LocalDate JUN_18 = date(2014, JUNE, 18);
  private static final LocalDate JUL_04 = date(2014, JULY, 4);
  private static final LocalDate JUL_05 = date(2014, JULY, 5);
  private static final LocalDate JUL_17 = date(2014, JULY, 17);
  private static final LocalDate JUL_18 = date(2014, JULY, 18);
  private static final LocalDate AUG_17 = date(2014, AUGUST, 17);  // Sunday
  private static final LocalDate AUG_18 = date(2014, AUGUST, 18);  // Monday
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17);
  private static final Offset<Double> TOLERANCE = within(1e-6);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> SchedulePeriod.of(null, JUL_18, JUL_04, JUL_17));
    assertThatIllegalArgumentException().isThrownBy(() -> SchedulePeriod.of(JUL_05, null, JUL_04, JUL_17));
    assertThatIllegalArgumentException().isThrownBy(() -> SchedulePeriod.of(JUL_05, JUL_18, null, JUL_17));
    assertThatIllegalArgumentException().isThrownBy(() -> SchedulePeriod.of(JUL_05, JUL_18, JUL_04, null));
    assertThatIllegalArgumentException().isThrownBy(() -> SchedulePeriod.of(null, null, null, null));
  }

  @Test
  public void test_of_all() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    assertThat(test.getStartDate()).isEqualTo(JUL_05);
    assertThat(test.getEndDate()).isEqualTo(JUL_18);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(JUL_04);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(JUL_17);
  }

  @Test
  public void test_of_noUnadjusted() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18);
    assertThat(test.getStartDate()).isEqualTo(JUL_05);
    assertThat(test.getEndDate()).isEqualTo(JUL_18);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(JUL_05);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(JUL_18);
  }

  @Test
  public void test_builder_defaults() {
    SchedulePeriod test = SchedulePeriod.builder()
        .startDate(JUL_05)
        .endDate(JUL_18)
        .build();
    assertThat(test.getStartDate()).isEqualTo(JUL_05);
    assertThat(test.getEndDate()).isEqualTo(JUL_18);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(JUL_05);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(JUL_18);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_yearFraction() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17);
    Schedule schedule = Schedule.ofTerm(test);
    assertThat(test.yearFraction(DayCounts.ACT_360, schedule))
        .isEqualTo(DayCounts.ACT_360.yearFraction(JUN_16, JUL_18, schedule), TOLERANCE);
  }

  @Test
  public void test_yearFraction_null() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17);
    Schedule schedule = Schedule.ofTerm(test);
    assertThatIllegalArgumentException().isThrownBy(() -> test.yearFraction(null, schedule));
    assertThatIllegalArgumentException().isThrownBy(() -> test.yearFraction(DayCounts.ACT_360, null));
    assertThatIllegalArgumentException().isThrownBy(() -> test.yearFraction(null, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_length() {
    assertThat(SchedulePeriod.of(JUN_16, JUN_18, JUN_16, JUN_18).length()).isEqualTo(Period.between(JUN_16, JUN_18));
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).length()).isEqualTo(Period.between(JUN_16, JUL_18));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_lengthInDays() {
    assertThat(SchedulePeriod.of(JUN_16, JUN_18, JUN_16, JUN_18).lengthInDays()).isEqualTo(2);
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).lengthInDays()).isEqualTo(32);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_isRegular() {
    assertThat(SchedulePeriod.of(JUN_18, JUL_18).isRegular(P1M, DAY_18)).isEqualTo(true);
    assertThat(SchedulePeriod.of(JUN_18, JUL_05).isRegular(P1M, DAY_18)).isEqualTo(false);
    assertThat(SchedulePeriod.of(JUL_05, JUL_18).isRegular(P1M, DAY_18)).isEqualTo(false);
    assertThat(SchedulePeriod.of(JUN_18, JUL_05).isRegular(P2M, DAY_18)).isEqualTo(false);
  }

  @Test
  public void test_isRegular_null() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18);
    assertThatIllegalArgumentException().isThrownBy(() -> test.isRegular(null, DAY_18));
    assertThatIllegalArgumentException().isThrownBy(() -> test.isRegular(P1M, null));
    assertThatIllegalArgumentException().isThrownBy(() -> test.isRegular(null, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_contains() {
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUN_15)).isEqualTo(false);
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUN_16)).isEqualTo(true);
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUL_05)).isEqualTo(true);
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUL_17)).isEqualTo(true);
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUL_18)).isEqualTo(false);
  }

  @Test
  public void test_contains_null() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18);
    assertThatIllegalArgumentException().isThrownBy(() -> test.contains(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_subSchedule_1monthIn3Month() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule = test.subSchedule(P1M, RollConventions.DAY_17, StubConvention.NONE, BusinessDayAdjustment.NONE)
        .createSchedule(REF_DATA);
    assertThat(schedule.size()).isEqualTo(3);
    assertThat(schedule.getPeriod(0)).isEqualTo(SchedulePeriod.of(JUN_17, JUL_17));
    assertThat(schedule.getPeriod(1)).isEqualTo(SchedulePeriod.of(JUL_17, AUG_17));
    assertThat(schedule.getPeriod(2)).isEqualTo(SchedulePeriod.of(AUG_17, SEP_17));
    assertThat(schedule.getFrequency()).isEqualTo(P1M);
    assertThat(schedule.getRollConvention()).isEqualTo(RollConventions.DAY_17);
  }

  @Test
  public void test_subSchedule_3monthIn3Month() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule =
        test.subSchedule(P3M, RollConventions.DAY_17, StubConvention.NONE, BusinessDayAdjustment.NONE)
            .createSchedule(REF_DATA);
    assertThat(schedule.size()).isEqualTo(1);
    assertThat(schedule.getPeriod(0)).isEqualTo(SchedulePeriod.of(JUN_17, SEP_17));
  }

  @Test
  public void test_subSchedule_2monthIn3Month_shortInitial() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule =
        test.subSchedule(P2M, RollConventions.DAY_17, StubConvention.SHORT_INITIAL, BusinessDayAdjustment.NONE)
            .createSchedule(REF_DATA);
    assertThat(schedule.size()).isEqualTo(2);
    assertThat(schedule.getPeriod(0)).isEqualTo(SchedulePeriod.of(JUN_17, JUL_17));
    assertThat(schedule.getPeriod(1)).isEqualTo(SchedulePeriod.of(JUL_17, SEP_17));
    assertThat(schedule.getFrequency()).isEqualTo(P2M);
    assertThat(schedule.getRollConvention()).isEqualTo(RollConventions.DAY_17);
  }

  @Test
  public void test_subSchedule_2monthIn3Month_shortFinal() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule =
        test.subSchedule(P2M, RollConventions.DAY_17, StubConvention.SHORT_FINAL, BusinessDayAdjustment.NONE)
            .createSchedule(REF_DATA);
    assertThat(schedule.size()).isEqualTo(2);
    assertThat(schedule.getPeriod(0)).isEqualTo(SchedulePeriod.of(JUN_17, AUG_17));
    assertThat(schedule.getPeriod(1)).isEqualTo(SchedulePeriod.of(AUG_17, SEP_17));
    assertThat(schedule.getFrequency()).isEqualTo(P2M);
    assertThat(schedule.getRollConvention()).isEqualTo(RollConventions.DAY_17);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toAdjusted() {
    SchedulePeriod test1 = SchedulePeriod.of(JUN_15, SEP_17);
    assertThat(test1.toAdjusted(date -> date)).isEqualTo(test1);
    assertThat(test1.toAdjusted(date -> date.equals(JUN_15) ? JUN_16 : date))
        .isEqualTo(SchedulePeriod.of(JUN_16, SEP_17, JUN_15, SEP_17));
    SchedulePeriod test2 = SchedulePeriod.of(JUN_16, AUG_17);
    assertThat(test2.toAdjusted(date -> date.equals(AUG_17) ? AUG_18 : date))
        .isEqualTo(SchedulePeriod.of(JUN_16, AUG_18, JUN_16, AUG_17));
  }

  @Test
  public void test_toUnadjusted() {
    assertThat(SchedulePeriod.of(JUN_15, SEP_17).toUnadjusted()).isEqualTo(SchedulePeriod.of(JUN_15, SEP_17));
    assertThat(SchedulePeriod.of(JUN_16, SEP_17, JUN_15, SEP_17).toUnadjusted()).isEqualTo(SchedulePeriod.of(JUN_15, SEP_17));
    assertThat(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).toUnadjusted()).isEqualTo(SchedulePeriod.of(JUN_16, JUL_17));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    SchedulePeriod a = SchedulePeriod.of(JUL_05, JUL_18);
    SchedulePeriod b = SchedulePeriod.of(JUL_04, JUL_18);
    SchedulePeriod c = SchedulePeriod.of(JUL_05, JUL_17);
    assertThat(a.compareTo(a) == 0).isEqualTo(true);
    assertThat(a.compareTo(b) > 0).isEqualTo(true);
    assertThat(a.compareTo(c) > 0).isEqualTo(true);

    assertThat(b.compareTo(a) < 0).isEqualTo(true);
    assertThat(b.compareTo(b) == 0).isEqualTo(true);
    assertThat(b.compareTo(c) < 0).isEqualTo(true);

    assertThat(c.compareTo(a) < 0).isEqualTo(true);
    assertThat(c.compareTo(b) > 0).isEqualTo(true);
    assertThat(c.compareTo(c) == 0).isEqualTo(true);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage_equals() {
    SchedulePeriod a1 = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    SchedulePeriod a2 = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    SchedulePeriod b = SchedulePeriod.of(JUL_04, JUL_18, JUL_04, JUL_17);
    SchedulePeriod c = SchedulePeriod.of(JUL_05, JUL_17, JUL_04, JUL_17);
    SchedulePeriod d = SchedulePeriod.of(JUL_05, JUL_18, JUL_05, JUL_17);
    SchedulePeriod e = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_18);
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
    assertThat(a1.equals(d)).isEqualTo(false);
    assertThat(a1.equals(e)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage_builder() {
    SchedulePeriod.Builder builder = SchedulePeriod.builder();
    builder
        .startDate(JUL_05)
        .endDate(JUL_18)
        .unadjustedStartDate(JUL_04)
        .unadjustedEndDate(JUL_17)
        .build();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    assertSerialization(test);
  }

}
