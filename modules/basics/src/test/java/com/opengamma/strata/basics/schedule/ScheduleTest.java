/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_17;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.Month.AUGUST;
import static java.time.Month.DECEMBER;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link Schedule}.
 */
public class ScheduleTest {

  private static final LocalDate JUN_15 = date(2014, JUNE, 15);
  private static final LocalDate JUN_16 = date(2014, JUNE, 16);
  private static final LocalDate JUL_03 = date(2014, JULY, 3);
  private static final LocalDate JUL_04 = date(2014, JULY, 4);
  private static final LocalDate JUL_16 = date(2014, JULY, 16);
  private static final LocalDate JUL_17 = date(2014, JULY, 17);
  private static final LocalDate AUG_16 = date(2014, AUGUST, 16);
  private static final LocalDate AUG_17 = date(2014, AUGUST, 17);
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17);
  private static final LocalDate SEP_30 = date(2014, SEPTEMBER, 30);
  private static final LocalDate OCT_15 = date(2014, OCTOBER, 15);
  private static final LocalDate OCT_17 = date(2014, OCTOBER, 17);
  private static final LocalDate NOV_17 = date(2014, NOVEMBER, 17);
  private static final LocalDate DEC_17 = date(2014, DECEMBER, 17);

  private static final SchedulePeriod P1_STUB = SchedulePeriod.of(JUL_03, JUL_17, JUL_04, JUL_17);
  private static final SchedulePeriod P2_NORMAL = SchedulePeriod.of(JUL_17, AUG_16, JUL_17, AUG_17);
  private static final SchedulePeriod P3_NORMAL = SchedulePeriod.of(AUG_16, SEP_17, AUG_17, SEP_17);
  private static final SchedulePeriod P4_STUB = SchedulePeriod.of(SEP_17, SEP_30);
  private static final SchedulePeriod P4_NORMAL = SchedulePeriod.of(SEP_17, OCT_17);
  private static final SchedulePeriod P5_NORMAL = SchedulePeriod.of(OCT_17, NOV_17);
  private static final SchedulePeriod P6_NORMAL = SchedulePeriod.of(NOV_17, DEC_17);

  private static final SchedulePeriod P1_2 = SchedulePeriod.of(JUL_03, AUG_16, JUL_04, AUG_17);
  private static final SchedulePeriod P1_3 = SchedulePeriod.of(JUL_03, SEP_17, JUL_04, SEP_17);
  private static final SchedulePeriod P2_3 = SchedulePeriod.of(JUL_17, SEP_17);
  private static final SchedulePeriod P3_4 = SchedulePeriod.of(AUG_16, OCT_17, AUG_17, OCT_17);
  private static final SchedulePeriod P3_4STUB = SchedulePeriod.of(AUG_16, SEP_30, AUG_17, SEP_30);
  private static final SchedulePeriod P4_5 = SchedulePeriod.of(SEP_17, NOV_17);
  private static final SchedulePeriod P5_6 = SchedulePeriod.of(OCT_17, DEC_17);

  private static final SchedulePeriod P2_4 = SchedulePeriod.of(JUL_17, OCT_17);
  private static final SchedulePeriod P4_6 = SchedulePeriod.of(SEP_17, DEC_17);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_size0() {
    assertThatIllegalArgumentException().isThrownBy(() -> Schedule.builder().periods(ImmutableList.of()));
  }

  @Test
  public void test_ofTerm() {
    Schedule test = Schedule.ofTerm(P1_STUB);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.isTerm()).isEqualTo(true);
    assertThat(test.isSinglePeriod()).isEqualTo(true);
    assertThat(test.getFrequency()).isEqualTo(TERM);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.NONE);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).isEqualTo(ImmutableList.of(P1_STUB));
    assertThat(test.getPeriod(0)).isEqualTo(P1_STUB);
    assertThat(test.getStartDate()).isEqualTo(P1_STUB.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P1_STUB.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P1_STUB.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P1_STUB.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P1_STUB);
    assertThat(test.getLastPeriod()).isEqualTo(P1_STUB);
    assertThat(test.getInitialStub()).isEqualTo(Optional.empty());
    assertThat(test.getFinalStub()).isEqualTo(Optional.empty());
    assertThat(test.getStubs(true)).isEqualTo(Pair.of(Optional.empty(), Optional.empty()));
    assertThat(test.getStubs(false)).isEqualTo(Pair.of(Optional.empty(), Optional.empty()));
    assertThat(test.getRegularPeriods()).isEqualTo(ImmutableList.of(P1_STUB));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(1));
    assertThat(test.getUnadjustedDates()).containsExactly(JUL_04, JUL_17);
  }

  @Test
  public void test_size1_stub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.isTerm()).isEqualTo(false);
    assertThat(test.isSinglePeriod()).isEqualTo(true);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getRollConvention()).isEqualTo(DAY_17);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).isEqualTo(ImmutableList.of(P1_STUB));
    assertThat(test.getPeriod(0)).isEqualTo(P1_STUB);
    assertThat(test.getStartDate()).isEqualTo(P1_STUB.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P1_STUB.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P1_STUB.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P1_STUB.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P1_STUB);
    assertThat(test.getLastPeriod()).isEqualTo(P1_STUB);
    assertThat(test.getInitialStub()).isEqualTo(Optional.of(P1_STUB));
    assertThat(test.getFinalStub()).isEqualTo(Optional.empty());
    assertThat(test.getStubs(true)).isEqualTo(Pair.of(Optional.empty(), Optional.of(P1_STUB)));
    assertThat(test.getStubs(false)).isEqualTo(Pair.of(Optional.of(P1_STUB), Optional.empty()));
    assertThat(test.getRegularPeriods()).isEqualTo(ImmutableList.of());
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(1));
    assertThat(test.getUnadjustedDates()).containsExactly(JUL_04, JUL_17);
  }

  @Test
  public void test_size1_noStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.isTerm()).isEqualTo(false);
    assertThat(test.isSinglePeriod()).isEqualTo(true);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getRollConvention()).isEqualTo(DAY_17);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).isEqualTo(ImmutableList.of(P2_NORMAL));
    assertThat(test.getPeriod(0)).isEqualTo(P2_NORMAL);
    assertThat(test.getStartDate()).isEqualTo(P2_NORMAL.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P2_NORMAL.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P2_NORMAL.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P2_NORMAL.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P2_NORMAL);
    assertThat(test.getLastPeriod()).isEqualTo(P2_NORMAL);
    assertThat(test.getInitialStub()).isEqualTo(Optional.empty());
    assertThat(test.getFinalStub()).isEqualTo(Optional.empty());
    assertThat(test.getStubs(true)).isEqualTo(Pair.of(Optional.empty(), Optional.empty()));
    assertThat(test.getStubs(false)).isEqualTo(Pair.of(Optional.empty(), Optional.empty()));
    assertThat(test.getRegularPeriods()).isEqualTo(ImmutableList.of(P2_NORMAL));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(1));
    assertThat(test.getUnadjustedDates()).containsExactly(JUL_17, AUG_17);
  }

  @Test
  public void test_of_size2_initialStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.isTerm()).isEqualTo(false);
    assertThat(test.isSinglePeriod()).isEqualTo(false);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getRollConvention()).isEqualTo(DAY_17);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).containsExactly(P1_STUB, P2_NORMAL);
    assertThat(test.getPeriod(0)).isEqualTo(P1_STUB);
    assertThat(test.getPeriod(1)).isEqualTo(P2_NORMAL);
    assertThat(test.getStartDate()).isEqualTo(P1_STUB.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P2_NORMAL.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P1_STUB.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P2_NORMAL.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P1_STUB);
    assertThat(test.getLastPeriod()).isEqualTo(P2_NORMAL);
    assertThat(test.getInitialStub()).isEqualTo(Optional.of(P1_STUB));
    assertThat(test.getFinalStub()).isEqualTo(Optional.empty());
    assertThat(test.getStubs(true)).isEqualTo(Pair.of(Optional.of(P1_STUB), Optional.empty()));
    assertThat(test.getStubs(false)).isEqualTo(Pair.of(Optional.of(P1_STUB), Optional.empty()));
    assertThat(test.getRegularPeriods()).isEqualTo(ImmutableList.of(P2_NORMAL));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(2));
    assertThat(test.getUnadjustedDates()).containsExactly(JUL_04, JUL_17, AUG_17);
  }

  @Test
  public void test_of_size2_noStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.isTerm()).isEqualTo(false);
    assertThat(test.isSinglePeriod()).isEqualTo(false);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getRollConvention()).isEqualTo(DAY_17);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).containsExactly(P2_NORMAL, P3_NORMAL);
    assertThat(test.getPeriod(0)).isEqualTo(P2_NORMAL);
    assertThat(test.getPeriod(1)).isEqualTo(P3_NORMAL);
    assertThat(test.getStartDate()).isEqualTo(P2_NORMAL.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P3_NORMAL.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P2_NORMAL.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P3_NORMAL.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P2_NORMAL);
    assertThat(test.getLastPeriod()).isEqualTo(P3_NORMAL);
    assertThat(test.getInitialStub()).isEqualTo(Optional.empty());
    assertThat(test.getFinalStub()).isEqualTo(Optional.empty());
    assertThat(test.getStubs(true)).isEqualTo(Pair.of(Optional.empty(), Optional.empty()));
    assertThat(test.getStubs(false)).isEqualTo(Pair.of(Optional.empty(), Optional.empty()));
    assertThat(test.getRegularPeriods()).containsExactly(P2_NORMAL, P3_NORMAL);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(2));
    assertThat(test.getUnadjustedDates()).containsExactly(JUL_17, AUG_17, SEP_17);
  }

  @Test
  public void test_of_size2_finalStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P3_NORMAL, P4_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.isTerm()).isEqualTo(false);
    assertThat(test.isSinglePeriod()).isEqualTo(false);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getRollConvention()).isEqualTo(DAY_17);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).containsExactly(P3_NORMAL, P4_STUB);
    assertThat(test.getPeriod(0)).isEqualTo(P3_NORMAL);
    assertThat(test.getPeriod(1)).isEqualTo(P4_STUB);
    assertThat(test.getStartDate()).isEqualTo(P3_NORMAL.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P4_STUB.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P3_NORMAL.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P4_STUB.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P3_NORMAL);
    assertThat(test.getLastPeriod()).isEqualTo(P4_STUB);
    assertThat(test.getInitialStub()).isEqualTo(Optional.empty());
    assertThat(test.getFinalStub()).isEqualTo(Optional.of(P4_STUB));
    assertThat(test.getStubs(true)).isEqualTo(Pair.of(Optional.empty(), Optional.of(P4_STUB)));
    assertThat(test.getStubs(false)).isEqualTo(Pair.of(Optional.empty(), Optional.of(P4_STUB)));
    assertThat(test.getRegularPeriods()).isEqualTo(ImmutableList.of(P3_NORMAL));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(2));
    assertThat(test.getUnadjustedDates()).containsExactly(AUG_17, SEP_17, SEP_30);
  }

  @Test
  public void test_of_size3_initialStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.isTerm()).isEqualTo(false);
    assertThat(test.isSinglePeriod()).isEqualTo(false);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getRollConvention()).isEqualTo(DAY_17);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).containsExactly(P1_STUB, P2_NORMAL, P3_NORMAL);
    assertThat(test.getPeriod(0)).isEqualTo(P1_STUB);
    assertThat(test.getPeriod(1)).isEqualTo(P2_NORMAL);
    assertThat(test.getPeriod(2)).isEqualTo(P3_NORMAL);
    assertThat(test.getStartDate()).isEqualTo(P1_STUB.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P3_NORMAL.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P1_STUB.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P3_NORMAL.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P1_STUB);
    assertThat(test.getLastPeriod()).isEqualTo(P3_NORMAL);
    assertThat(test.getInitialStub()).isEqualTo(Optional.of(P1_STUB));
    assertThat(test.getFinalStub()).isEqualTo(Optional.empty());
    assertThat(test.getRegularPeriods()).containsExactly(P2_NORMAL, P3_NORMAL);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(3));
    assertThat(test.getUnadjustedDates()).containsExactly(JUL_04, JUL_17, AUG_17, SEP_17);
  }

  @Test
  public void test_of_size4_bothStubs() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL, P4_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.size()).isEqualTo(4);
    assertThat(test.isTerm()).isEqualTo(false);
    assertThat(test.isSinglePeriod()).isEqualTo(false);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getRollConvention()).isEqualTo(DAY_17);
    assertThat(test.isEndOfMonthConvention()).isEqualTo(false);
    assertThat(test.getPeriods()).containsExactly(P1_STUB, P2_NORMAL, P3_NORMAL, P4_STUB);
    assertThat(test.getPeriod(0)).isEqualTo(P1_STUB);
    assertThat(test.getPeriod(1)).isEqualTo(P2_NORMAL);
    assertThat(test.getPeriod(2)).isEqualTo(P3_NORMAL);
    assertThat(test.getPeriod(3)).isEqualTo(P4_STUB);
    assertThat(test.getStartDate()).isEqualTo(P1_STUB.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(P4_STUB.getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(P1_STUB.getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(P4_STUB.getUnadjustedEndDate());
    assertThat(test.getFirstPeriod()).isEqualTo(P1_STUB);
    assertThat(test.getLastPeriod()).isEqualTo(P4_STUB);
    assertThat(test.getInitialStub()).isEqualTo(Optional.of(P1_STUB));
    assertThat(test.getFinalStub()).isEqualTo(Optional.of(P4_STUB));
    assertThat(test.getRegularPeriods()).containsExactly(P2_NORMAL, P3_NORMAL);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getPeriod(4));
    assertThat(test.getUnadjustedDates()).containsExactly(JUL_04, JUL_17, AUG_17, SEP_17, SEP_30);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_isEndOfMonthConvention_eom() {
    // schedule doesn't make sense, but test only requires roll convention of EOM
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(RollConventions.EOM)
        .build();
    assertThat(test.isEndOfMonthConvention()).isEqualTo(true);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getPeriodEndDate() {
    // schedule doesn't make sense, but test only requires roll convention of EOM
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.getPeriodEndDate(P2_NORMAL.getStartDate())).isEqualTo(P2_NORMAL.getEndDate());
    assertThat(test.getPeriodEndDate(P2_NORMAL.getStartDate().plusDays(1))).isEqualTo(P2_NORMAL.getEndDate());
    assertThat(test.getPeriodEndDate(P3_NORMAL.getStartDate())).isEqualTo(P3_NORMAL.getEndDate());
    assertThat(test.getPeriodEndDate(P3_NORMAL.getStartDate().plusDays(1))).isEqualTo(P3_NORMAL.getEndDate());
    assertThatIllegalArgumentException().isThrownBy(() -> test.getPeriodEndDate(P2_NORMAL.getStartDate().minusDays(1)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mergeToTerm() {
    Schedule testNormal = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(testNormal.mergeToTerm()).isEqualTo(Schedule.ofTerm(P1_3));
    assertThat(testNormal.mergeToTerm().mergeToTerm()).isEqualTo(Schedule.ofTerm(P1_3));
  }

  @Test
  public void test_mergeToTerm_size1_stub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeToTerm()).isEqualTo(Schedule.ofTerm(P1_STUB));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_merge_group2_within2_initialStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_3))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, true)).isEqualTo(expected);
    assertThat(test.mergeRegular(2, false)).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getUnadjustedStartDate(), P3_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getStartDate(), P3_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within2_noStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, true)).isEqualTo(expected);
    assertThat(test.mergeRegular(2, false)).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getUnadjustedStartDate(), P3_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getStartDate(), P3_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within2_finalStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_STUB))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, true)).isEqualTo(expected);
    assertThat(test.mergeRegular(2, false)).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getUnadjustedStartDate(), P3_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getStartDate(), P3_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within3_forwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_NORMAL))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, true)).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getUnadjustedStartDate(), P3_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getStartDate(), P3_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within3_backwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_4))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, false)).isEqualTo(expected);
    assertThat(test.merge(2, P3_NORMAL.getUnadjustedStartDate(), P4_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P3_NORMAL.getStartDate(), P4_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within5_forwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_5, P6_NORMAL))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, true)).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getUnadjustedStartDate(), P5_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P2_NORMAL.getStartDate(), P5_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within5_backwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_4, P5_6))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, false)).isEqualTo(expected);
    assertThat(test.merge(2, P3_NORMAL.getUnadjustedStartDate(), P6_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P3_NORMAL.getStartDate(), P6_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within6_includeInitialStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P1_2, P3_4, P5_6))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.merge(2, P3_NORMAL.getUnadjustedStartDate(), P6_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P3_NORMAL.getStartDate(), P6_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group2_within6_includeFinalStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL, P4_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P1_2, P3_4STUB))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.merge(2, P1_STUB.getUnadjustedStartDate(), P2_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(2, P1_STUB.getStartDate(), P2_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group3_within5_forwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_4, P5_6))
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(3, true)).isEqualTo(expected);
    assertThat(test.merge(3, P2_NORMAL.getUnadjustedStartDate(), P4_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(3, P2_NORMAL.getStartDate(), P4_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_group3_within5_backwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_6))
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(3, false)).isEqualTo(expected);
    assertThat(test.merge(3, P4_NORMAL.getUnadjustedStartDate(), P6_NORMAL.getUnadjustedEndDate())).isEqualTo(expected);
    assertThat(test.merge(3, P4_NORMAL.getStartDate(), P6_NORMAL.getEndDate())).isEqualTo(expected);
  }

  @Test
  public void test_merge_termNoChange() {
    Schedule test = Schedule.ofTerm(P1_STUB);
    assertThat(test.mergeRegular(2, true)).isEqualTo(test);
    assertThat(test.mergeRegular(2, false)).isEqualTo(test);
    assertThat(test.merge(2, P1_STUB.getUnadjustedStartDate(), P1_STUB.getUnadjustedEndDate())).isEqualTo(test);
    assertThat(test.merge(2, P1_STUB.getStartDate(), P1_STUB.getEndDate())).isEqualTo(test);
  }

  @Test
  public void test_merge_size1_stub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(2, true)).isEqualTo(test);
    assertThat(test.mergeRegular(2, false)).isEqualTo(test);
    assertThat(test.merge(2, P1_STUB.getUnadjustedStartDate(), P1_STUB.getUnadjustedEndDate())).isEqualTo(test);
    assertThat(test.merge(2, P1_STUB.getStartDate(), P1_STUB.getEndDate())).isEqualTo(test);
  }

  @Test
  public void test_merge_groupSizeOneNoChange() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.mergeRegular(1, true)).isEqualTo(test);
    assertThat(test.mergeRegular(1, false)).isEqualTo(test);
    assertThat(test.merge(1, P2_NORMAL.getUnadjustedStartDate(), P6_NORMAL.getUnadjustedEndDate())).isEqualTo(test);
    assertThat(test.merge(1, P2_NORMAL.getStartDate(), P6_NORMAL.getEndDate())).isEqualTo(test);
  }

  @Test
  public void test_merge_groupSizeInvalid() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThatIllegalArgumentException().isThrownBy(() -> test.mergeRegular(0, true));
    assertThatIllegalArgumentException().isThrownBy(() -> test.mergeRegular(0, false));
    assertThatIllegalArgumentException().isThrownBy(() -> test.mergeRegular(-1, true));
    assertThatIllegalArgumentException().isThrownBy(() -> test.mergeRegular(-1, false));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.merge(0, P2_NORMAL.getUnadjustedStartDate(), P6_NORMAL.getUnadjustedEndDate()));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.merge(-1, P2_NORMAL.getUnadjustedStartDate(), P6_NORMAL.getUnadjustedEndDate()));
  }

  @Test
  public void test_merge_badDate() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> test.merge(2, JUL_03, AUG_17));
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> test.merge(2, JUL_17, SEP_30));
  }

  @Test
  public void test_merge_badGroupSize() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThatExceptionOfType(ScheduleException.class)
        .isThrownBy(() -> test.merge(2, P2_NORMAL.getUnadjustedStartDate(), P6_NORMAL.getUnadjustedEndDate()))
        .withMessage(
            "Unable to merge schedule, firstRegularStartDate " + P2_NORMAL.getUnadjustedStartDate() +
                " and lastRegularEndDate " + P6_NORMAL.getUnadjustedEndDate() +
                " cannot be used to create regular periods of frequency 'P2M'");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toAdjusted() {
    SchedulePeriod period1 = SchedulePeriod.of(JUN_15, SEP_17);
    SchedulePeriod period2 = SchedulePeriod.of(SEP_17, SEP_30);
    Schedule test = Schedule.builder()
        .periods(period1, period2)
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test.toAdjusted(date -> date)).isEqualTo(test);
    assertThat(test.toAdjusted(date -> date.equals(JUN_15) ? JUN_16 : date))
        .isEqualTo(Schedule.builder()
            .periods(SchedulePeriod.of(JUN_16, SEP_17, JUN_15, SEP_17), period2)
            .frequency(P3M)
            .rollConvention(DAY_17)
            .build());
  }

  @Test
  public void test_toUnadjusted() {
    SchedulePeriod a = SchedulePeriod.of(JUL_17, OCT_17, JUL_16, OCT_15);
    SchedulePeriod b = SchedulePeriod.of(JUL_16, OCT_15, JUL_16, OCT_15);
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(a))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build()
        .toUnadjusted();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(b))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage_equals() {
    Schedule a = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule b = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule c = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    Schedule d = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(RollConventions.DAY_1)
        .build();
    assertThat(a.equals(a)).isEqualTo(true);
    assertThat(a.equals(b)).isEqualTo(false);
    assertThat(a.equals(c)).isEqualTo(false);
    assertThat(a.equals(d)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage_builder() {
    Schedule.Builder builder = Schedule.builder();
    builder
        .periods(P1_STUB)
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertSerialization(test);
  }

}
