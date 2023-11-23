/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_PRECEDING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P1D;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P1W;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_11;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_17;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_22;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_24;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_28;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_29;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_30;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_4;
import static com.opengamma.strata.basics.schedule.RollConventions.EOM;
import static com.opengamma.strata.basics.schedule.RollConventions.IMM;
import static com.opengamma.strata.basics.schedule.RollConventions.SFE;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_INITIAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_INITIAL;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.list;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.HolidayCalendars;

/**
 * Test {@link PeriodicSchedule}.
 */
public class PeriodicScheduleTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final RollConvention ROLL_NONE = RollConventions.NONE;
  private static final StubConvention STUB_NONE = StubConvention.NONE;
  private static final StubConvention STUB_BOTH = StubConvention.BOTH;
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
  private static final BusinessDayAdjustment BDA_JPY_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO);
  private static final BusinessDayAdjustment BDA_JPY_P = BusinessDayAdjustment.of(PRECEDING, JPTO);
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final LocalDate NOV_29_2013 = date(2013, NOVEMBER, 29);  // Fri
  private static final LocalDate NOV_30_2013 = date(2013, NOVEMBER, 30);  // Sat
  private static final LocalDate FEB_28 = date(2014, FEBRUARY, 28); // Fri
  private static final LocalDate APR_01 = date(2014, APRIL, 1); // Tue
  private static final LocalDate MAY_17 = date(2014, MAY, 17);  // Sat
  private static final LocalDate MAY_19 = date(2014, MAY, 19);  // Mon
  private static final LocalDate MAY_30 = date(2014, MAY, 30);  // Fri
  private static final LocalDate MAY_31 = date(2014, MAY, 31);  // Sat
  private static final LocalDate JUN_03 = date(2014, JUNE, 3);  // Tue
  private static final LocalDate JUN_04 = date(2014, JUNE, 4);  // Wed
  private static final LocalDate JUN_10 = date(2014, JUNE, 10);  // Tue
  private static final LocalDate JUN_11 = date(2014, JUNE, 11);  // Wed
  private static final LocalDate JUN_17 = date(2014, JUNE, 17);  // Tue
  private static final LocalDate JUL_04 = date(2014, JULY, 4); // Fri
  private static final LocalDate JUL_11 = date(2014, JULY, 11); // Fri
  private static final LocalDate JUL_17 = date(2014, JULY, 17); // Thu
  private static final LocalDate JUL_30 = date(2014, JULY, 30);  // Wed
  private static final LocalDate AUG_04 = date(2014, AUGUST, 4); // Mon
  private static final LocalDate AUG_11 = date(2014, AUGUST, 11); // Mon
  private static final LocalDate AUG_17 = date(2014, AUGUST, 17); // Sun
  private static final LocalDate AUG_18 = date(2014, AUGUST, 18); // Mon
  private static final LocalDate AUG_29 = date(2014, AUGUST, 29);  // Fri
  private static final LocalDate AUG_30 = date(2014, AUGUST, 30);  // Sat
  private static final LocalDate AUG_31 = date(2014, AUGUST, 31);  // Sun
  private static final LocalDate SEP_04 = date(2014, SEPTEMBER, 4); // Thu
  private static final LocalDate SEP_05 = date(2014, SEPTEMBER, 5); // Fri
  private static final LocalDate SEP_10 = date(2014, SEPTEMBER, 10); // Wed
  private static final LocalDate SEP_11 = date(2014, SEPTEMBER, 11); // Thu
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17); // Wed
  private static final LocalDate SEP_18 = date(2014, SEPTEMBER, 18); // Thu
  private static final LocalDate SEP_30 = date(2014, SEPTEMBER, 30);  // Tue
  private static final LocalDate OCT_17 = date(2014, OCTOBER, 17); // Fri
  private static final LocalDate OCT_30 = date(2014, OCTOBER, 30);  // Thu
  private static final LocalDate NOV_28 = date(2014, NOVEMBER, 28);  // Fri
  private static final LocalDate NOV_30 = date(2014, NOVEMBER, 30);  // Sun

  //-------------------------------------------------------------------------
  @Test
  public void test_of_LocalDateEomFalse() {
    PeriodicSchedule test = PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, false);
    assertThat(test.getStartDate()).isEqualTo(JUN_04);
    assertThat(test.getEndDate()).isEqualTo(SEP_17);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(test.getStubConvention()).isEqualTo(Optional.of(SHORT_INITIAL));
    assertThat(test.getRollConvention()).isEqualTo(Optional.empty());
    assertThat(test.getFirstRegularStartDate()).isEqualTo(Optional.empty());
    assertThat(test.getLastRegularEndDate()).isEqualTo(Optional.empty());
    assertThat(test.getOverrideStartDate()).isEqualTo(Optional.empty());
    assertThat(test.calculatedRollConvention()).isEqualTo(DAY_17);
    assertThat(test.calculatedFirstRegularStartDate()).isEqualTo(JUN_04);
    assertThat(test.calculatedLastRegularEndDate()).isEqualTo(SEP_17);
    assertThat(test.calculatedStartDate()).isEqualTo(AdjustableDate.of(JUN_04, BDA));
    assertThat(test.calculatedEndDate()).isEqualTo(AdjustableDate.of(SEP_17, BDA));
  }

  @Test
  public void test_of_LocalDateEomTrue() {
    PeriodicSchedule test = PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_FINAL, true);
    assertThat(test.getStartDate()).isEqualTo(JUN_04);
    assertThat(test.getEndDate()).isEqualTo(SEP_17);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(test.getStubConvention()).isEqualTo(Optional.of(SHORT_FINAL));
    assertThat(test.getRollConvention()).isEqualTo(Optional.of(EOM));
    assertThat(test.getFirstRegularStartDate()).isEqualTo(Optional.empty());
    assertThat(test.getLastRegularEndDate()).isEqualTo(Optional.empty());
    assertThat(test.getOverrideStartDate()).isEqualTo(Optional.empty());
    assertThat(test.calculatedRollConvention()).isEqualTo(DAY_4);
    assertThat(test.calculatedFirstRegularStartDate()).isEqualTo(JUN_04);
    assertThat(test.calculatedLastRegularEndDate()).isEqualTo(SEP_17);
    assertThat(test.calculatedStartDate()).isEqualTo(AdjustableDate.of(JUN_04, BDA));
    assertThat(test.calculatedEndDate()).isEqualTo(AdjustableDate.of(SEP_17, BDA));
  }

  @Test
  public void test_of_LocalDateEom_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PeriodicSchedule.of(null, SEP_17, P1M, BDA, SHORT_INITIAL, false));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PeriodicSchedule.of(JUN_04, null, P1M, BDA, SHORT_INITIAL, false));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PeriodicSchedule.of(JUN_04, SEP_17, null, BDA, SHORT_INITIAL, false));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, null, SHORT_INITIAL, false));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, null, false));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_LocalDateRoll() {
    PeriodicSchedule test = PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17);
    assertThat(test.getStartDate()).isEqualTo(JUN_04);
    assertThat(test.getEndDate()).isEqualTo(SEP_17);
    assertThat(test.getFrequency()).isEqualTo(P1M);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(test.getStubConvention()).isEqualTo(Optional.of(SHORT_INITIAL));
    assertThat(test.getRollConvention()).isEqualTo(Optional.of(DAY_17));
    assertThat(test.getFirstRegularStartDate()).isEqualTo(Optional.empty());
    assertThat(test.getLastRegularEndDate()).isEqualTo(Optional.empty());
    assertThat(test.getOverrideStartDate()).isEqualTo(Optional.empty());
    assertThat(test.calculatedRollConvention()).isEqualTo(DAY_17);
    assertThat(test.calculatedFirstRegularStartDate()).isEqualTo(JUN_04);
    assertThat(test.calculatedLastRegularEndDate()).isEqualTo(SEP_17);
    assertThat(test.calculatedStartDate()).isEqualTo(AdjustableDate.of(JUN_04, BDA));
    assertThat(test.calculatedEndDate()).isEqualTo(AdjustableDate.of(SEP_17, BDA));
  }

  @Test
  public void test_firstPaymentDate_before_effectiveDate() {

    // Schedule where the combination of override start date and regular first period start date produce a first
    // payment date which is before the (non-overridden) start date

    LocalDate startDate = LocalDate.of(2018, 7, 26);
    LocalDate endDate = LocalDate.of(2019, 6, 20);
    LocalDate overrideStartDate = LocalDate.of(2018, 3, 20);
    LocalDate firstRegularStartDate = LocalDate.of(2018, 6, 20);

    PeriodicSchedule scheduleDefinition = PeriodicSchedule.builder()
        .startDate(startDate)
        .endDate(endDate)
        .frequency(Frequency.P3M)
        .businessDayAdjustment(BDA)
        .firstRegularStartDate(firstRegularStartDate)
        .overrideStartDate(AdjustableDate.of(overrideStartDate))
        .build();

    Schedule schedule = scheduleDefinition.createSchedule(REF_DATA);
    assertThat(schedule.size()).isEqualTo(5);

    for (int i = 0; i < schedule.size(); i++) {

      LocalDate expectedStart = overrideStartDate.plusMonths(3 * i);
      LocalDate expectedEnd = expectedStart.plusMonths(3);
      SchedulePeriod expectedPeriod = SchedulePeriod.of(expectedStart, expectedEnd);

      SchedulePeriod actualPeriod = schedule.getPeriod(i);
      assertThat(expectedPeriod).isEqualTo(actualPeriod);
    }
  }

  @Test
  public void test_of_LocalDateRoll_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodicSchedule.of(null, SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodicSchedule.of(JUN_04, null, P1M, BDA, SHORT_INITIAL, DAY_17));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodicSchedule.of(JUN_04, SEP_17, null, BDA, SHORT_INITIAL, DAY_17));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, null, SHORT_INITIAL, DAY_17));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, null, DAY_17));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_invalidDateOrder() {
    // start vs end
    assertThatIllegalArgumentException().isThrownBy(() -> createDates(SEP_17, SEP_17, null, null));
    assertThatIllegalArgumentException().isThrownBy(() -> createDates(SEP_17, JUN_04, null, null));
    // first/last regular vs start/end
    assertThatIllegalArgumentException().isThrownBy(() -> createDates(JUN_04, SEP_17, JUN_03, null));
    assertThatIllegalArgumentException().isThrownBy(() -> createDates(JUN_04, SEP_17, null, SEP_18));
    // first regular vs last regular
    createDates(JUN_04, SEP_05, SEP_05, SEP_05);  // allow this
    assertThatIllegalArgumentException().isThrownBy(() -> createDates(JUN_04, SEP_17, SEP_05, SEP_04));
    // first regular vs override start date
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodicSchedule.builder()
        .startDate(JUN_04)
        .endDate(SEP_17)
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .firstRegularStartDate(JUL_17)
        .overrideStartDate(AdjustableDate.of(AUG_04))
        .build());
  }

  private PeriodicSchedule createDates(LocalDate start, LocalDate end, LocalDate first, LocalDate last) {
    return PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .firstRegularStartDate(first)
        .lastRegularEndDate(last)
        .build();
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_generation() {
    return new Object[][] {
        // stub null
        {JUN_17, SEP_17, P1M, null, null, BDA, null, null, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},

        // stub NONE
        {JUN_17, SEP_17, P1M, STUB_NONE, null, BDA, null, null, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, JUL_17, P1M, STUB_NONE, null, BDA, null, null, null,
            list(JUN_17, JUL_17),
            list(JUN_17, JUL_17), DAY_17},

        // stub SHORT_INITIAL
        {JUN_04, SEP_17, P1M, SHORT_INITIAL, null, BDA, null, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, SEP_17, P1M, SHORT_INITIAL, null, BDA, null, null, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, JUL_04, P1M, SHORT_INITIAL, null, BDA, null, null, null,
            list(JUN_17, JUL_04),
            list(JUN_17, JUL_04), DAY_4},
        {date(2011, 6, 28), date(2011, 6, 30), P1M, SHORT_INITIAL, EOM, BDA, null, null, null,
            list(date(2011, 6, 28), date(2011, 6, 30)),
            list(date(2011, 6, 28), date(2011, 6, 30)), EOM},
        {date(2014, 12, 12), date(2015, 8, 24), P3M, SHORT_INITIAL, null, BDA, null, null, null,
            list(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
            list(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24)), DAY_24},
        {date(2014, 12, 12), date(2015, 8, 24), P3M, SHORT_INITIAL, RollConventions.NONE, BDA, null, null, null,
            list(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
            list(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24)), DAY_24},
        {date(2014, 11, 24), date(2015, 8, 24), P3M, null, RollConventions.NONE, BDA, null, null, null,
            list(date(2014, 11, 24), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
            list(date(2014, 11, 24), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24)), DAY_24},

        // stub LONG_INITIAL
        {JUN_04, SEP_17, P1M, LONG_INITIAL, null, BDA, null, null, null,
            list(JUN_04, JUL_17, AUG_17, SEP_17),
            list(JUN_04, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, SEP_17, P1M, LONG_INITIAL, null, BDA, null, null, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, JUL_04, P1M, LONG_INITIAL, null, BDA, null, null, null,
            list(JUN_17, JUL_04),
            list(JUN_17, JUL_04), DAY_4},
        {JUN_17, AUG_04, P1M, LONG_INITIAL, null, BDA, null, null, null,
            list(JUN_17, AUG_04),
            list(JUN_17, AUG_04), DAY_4},

        // stub SMART_INITIAL
        {JUN_04, SEP_17, P1M, SMART_INITIAL, null, BDA, null, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_10, SEP_17, P1M, SMART_INITIAL, null, BDA, null, null, null,
            list(JUN_10, JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_10, JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_11, SEP_17, P1M, SMART_INITIAL, null, BDA, null, null, null,
            list(JUN_11, JUL_17, AUG_17, SEP_17),
            list(JUN_11, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, JUL_04, P1M, SMART_INITIAL, null, BDA, null, null, null,
            list(JUN_17, JUL_04),
            list(JUN_17, JUL_04), DAY_4},

        // stub SHORT_FINAL
        {JUN_04, SEP_17, P1M, SHORT_FINAL, null, BDA, null, null, null,
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17),
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17), DAY_4},
        {JUN_17, SEP_17, P1M, SHORT_FINAL, null, BDA, null, null, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, JUL_04, P1M, SHORT_FINAL, null, BDA, null, null, null,
            list(JUN_17, JUL_04),
            list(JUN_17, JUL_04), DAY_17},
        {date(2011, 6, 28), date(2011, 6, 30), P1M, SHORT_FINAL, EOM, BDA, null, null, null,
            list(date(2011, 6, 28), date(2011, 6, 30)),
            list(date(2011, 6, 28), date(2011, 6, 30)), DAY_28},
        {date(2014, 11, 29), date(2015, 9, 2), P3M, SHORT_FINAL, null, BDA, null, null, null,
            list(date(2014, 11, 29), date(2015, 2, 28), date(2015, 5, 29), date(2015, 8, 29), date(2015, 9, 2)),
            list(date(2014, 11, 28), date(2015, 2, 27), date(2015, 5, 29), date(2015, 8, 31), date(2015, 9, 2)),
            DAY_29},
        {date(2014, 11, 29), date(2015, 9, 2), P3M, SHORT_FINAL, RollConventions.NONE, BDA, null, null, null,
            list(date(2014, 11, 29), date(2015, 2, 28), date(2015, 5, 29), date(2015, 8, 29), date(2015, 9, 2)),
            list(date(2014, 11, 28), date(2015, 2, 27), date(2015, 5, 29), date(2015, 8, 31), date(2015, 9, 2)),
            DAY_29},

        // stub LONG_FINAL
        {JUN_04, SEP_17, P1M, LONG_FINAL, null, BDA, null, null, null,
            list(JUN_04, JUL_04, AUG_04, SEP_17),
            list(JUN_04, JUL_04, AUG_04, SEP_17), DAY_4},
        {JUN_17, SEP_17, P1M, LONG_FINAL, null, BDA, null, null, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, JUL_04, P1M, LONG_FINAL, null, BDA, null, null, null,
            list(JUN_17, JUL_04),
            list(JUN_17, JUL_04), DAY_17},
        {JUN_17, AUG_04, P1M, LONG_FINAL, null, BDA, null, null, null,
            list(JUN_17, AUG_04),
            list(JUN_17, AUG_04), DAY_17},

        // stub SMART_FINAL
        {JUN_04, SEP_17, P1M, SMART_FINAL, null, BDA, null, null, null,
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17),
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17), DAY_4},
        {JUN_04, SEP_11, P1M, SMART_FINAL, null, BDA, null, null, null,
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_11),
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_11), DAY_4},
        {JUN_04, SEP_10, P1M, SMART_FINAL, null, BDA, null, null, null,
            list(JUN_04, JUL_04, AUG_04, SEP_10),
            list(JUN_04, JUL_04, AUG_04, SEP_10), DAY_4},
        {JUN_17, JUL_04, P1M, SMART_FINAL, null, BDA, null, null, null,
            list(JUN_17, JUL_04),
            list(JUN_17, JUL_04), DAY_17},

        // explicit initial stub
        {JUN_04, SEP_17, P1M, null, null, BDA, JUN_17, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_04, SEP_17, P1M, SHORT_INITIAL, null, BDA, JUN_17, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_17, SEP_17, P1M, null, null, BDA, JUN_17, null, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_04, SEP_04, P1M, SMART_FINAL, null, BDA, JUN_17, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, SEP_04),
            list(JUN_04, JUN_17, JUL_17, AUG_18, SEP_04), DAY_17},

        // explicit final stub
        {JUN_04, SEP_17, P1M, null, null, BDA, null, AUG_04, null,
            list(JUN_04, JUL_04, AUG_04, SEP_17),
            list(JUN_04, JUL_04, AUG_04, SEP_17), DAY_4},
        {JUN_04, SEP_17, P1M, SHORT_FINAL, null, BDA, null, AUG_04, null,
            list(JUN_04, JUL_04, AUG_04, SEP_17),
            list(JUN_04, JUL_04, AUG_04, SEP_17), DAY_4},
        {JUN_17, SEP_17, P1M, null, null, BDA, null, AUG_17, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_04, SEP_04, P1M, SMART_INITIAL, null, BDA, null, AUG_17, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, SEP_04),
            list(JUN_04, JUN_17, JUL_17, AUG_18, SEP_04), DAY_17},

        // explicit double stub
        {JUN_04, SEP_17, P1M, null, null, BDA, JUL_11, AUG_11, null,
            list(JUN_04, JUL_11, AUG_11, SEP_17),
            list(JUN_04, JUL_11, AUG_11, SEP_17), DAY_11},
        {JUN_04, OCT_17, P1M, STUB_BOTH, null, BDA, JUL_11, SEP_11, null,
            list(JUN_04, JUL_11, AUG_11, SEP_11, OCT_17),
            list(JUN_04, JUL_11, AUG_11, SEP_11, OCT_17), DAY_11},
        {JUN_17, SEP_17, P1M, null, null, BDA, JUN_17, SEP_17, null,
            list(JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},

        // stub null derive from roll convention
        {JUN_04, SEP_17, P1M, null, DAY_17, BDA, null, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            list(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17), DAY_17},
        {JUN_04, SEP_17, P1M, null, DAY_4, BDA, null, null, null,
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17),
            list(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17), DAY_4},

        // near end of month
        // EOM flag false, thus roll on 30th
        {NOV_30_2013, NOV_30, P3M, STUB_NONE, null, BDA, null, null, null,
            list(NOV_30_2013, FEB_28, MAY_30, AUG_30, NOV_30),
            list(NOV_29_2013, FEB_28, MAY_30, AUG_29, NOV_28), DAY_30},
        // EOM flag true and is EOM, thus roll at EOM
        {NOV_30_2013, NOV_30, P3M, STUB_NONE, EOM, BDA, null, null, null,
            list(NOV_30_2013, FEB_28, MAY_31, AUG_31, NOV_30),
            list(NOV_29_2013, FEB_28, MAY_30, AUG_29, NOV_28), EOM},
        // EOM flag true, and last business day, thus roll at EOM (stub convention defined)
        {MAY_30, NOV_30, P3M, STUB_NONE, EOM, BDA, null, null, null,
            list(MAY_31, AUG_31, NOV_30),
            list(MAY_30, AUG_29, NOV_28), EOM},
        // EOM flag true, and last business day, thus roll at EOM
        {MAY_30, NOV_30, P3M, null, EOM, BDA, null, null, null,
            list(MAY_31, AUG_31, NOV_30),
            list(MAY_30, AUG_29, NOV_28), EOM},
        // EOM flag true, and last business day, thus roll at EOM (start adjustment none)
        {MAY_30, NOV_30, P3M, null, EOM, BDA, null, null, BDA_NONE,
            list(MAY_31, AUG_31, NOV_30),
            list(MAY_30, AUG_29, NOV_28), EOM},
        // roll date set to 30th, so roll on 30th
        {MAY_30, NOV_30, P3M, null, DAY_30, BDA, null, null, null,
            list(MAY_30, AUG_30, NOV_30),
            list(MAY_30, AUG_29, NOV_28), DAY_30},
        // EOM flag true, but not EOM, thus roll on 30th
        {JUL_30, OCT_30, P1M, null, EOM, BDA, null, null, null,
            list(JUL_30, AUG_30, SEP_30, OCT_30),
            list(JUL_30, AUG_29, SEP_30, OCT_30), DAY_30},
        // EOM flag true and is EOM, double stub, thus roll at EOM
        {date(2014, 1, 3), SEP_17, P3M, STUB_BOTH, EOM, BDA, FEB_28, AUG_31, null,
            list(date(2014, 1, 3), FEB_28, MAY_31, AUG_31, SEP_17),
            list(date(2014, 1, 3), FEB_28, MAY_30, AUG_29, SEP_17), EOM},
        // EOM flag true plus start date as last business day of month with start date adjust of NONE
        {NOV_29_2013, NOV_30, P3M, STUB_NONE, EOM, BDA, null, null, BDA_NONE,
            list(NOV_30_2013, FEB_28, MAY_31, AUG_31, NOV_30),
            list(NOV_29_2013, FEB_28, MAY_30, AUG_29, NOV_28), EOM},
        // EOM flag true plus start date as last business day of month with start date adjust of NONE
        {NOV_29_2013, NOV_30, P3M, null, EOM, BDA, null, null, BDA_NONE,
            list(NOV_30_2013, FEB_28, MAY_31, AUG_31, NOV_30),
            list(NOV_29_2013, FEB_28, MAY_30, AUG_29, NOV_28), EOM},
        // EOM flag false, short initial, implies EOM true
        {date(2011, 6, 2), date(2011, 8, 31), P1M, SHORT_INITIAL, null, BDA, null, null, null,
            list(date(2011, 6, 2), date(2011, 6, 30), date(2011, 7, 31), date(2011, 8, 31)),
            list(date(2011, 6, 2), date(2011, 6, 30), date(2011, 7, 29), date(2011, 8, 31)), EOM},
        // EOM flag false, explicit stub, implies EOM true
        {date(2011, 6, 2), date(2011, 8, 31), P1M, null, null, BDA, date(2011, 6, 30), null, null,
            list(date(2011, 6, 2), date(2011, 6, 30), date(2011, 7, 31), date(2011, 8, 31)),
            list(date(2011, 6, 2), date(2011, 6, 30), date(2011, 7, 29), date(2011, 8, 31)), EOM},
        // EOM flag false, explicit stub, implies EOM true
        {date(2011, 7, 31), date(2011, 10, 10), P1M, null, null, BDA, null, date(2011, 9, 30), null,
            list(date(2011, 7, 31), date(2011, 8, 31), date(2011, 9, 30), date(2011, 10, 10)),
            list(date(2011, 7, 29), date(2011, 8, 31), date(2011, 9, 30), date(2011, 10, 10)), EOM},
        // EOM flag false, explicit stub, implies EOM true
        {date(2011, 2, 2), date(2011, 5, 30), P1M, null, null, BDA, date(2011, 2, 28), null, null,
            list(date(2011, 2, 2), date(2011, 2, 28), date(2011, 3, 30), date(2011, 4, 30), date(2011, 5, 30)),
            list(date(2011, 2, 2), date(2011, 2, 28), date(2011, 3, 30), date(2011, 4, 29), date(2011, 5, 30)),
            DAY_30},
        // EOM flag true and is EOM, but end date equals start day rather than EOM
        {date(2018, 2, 28), date(2024, 2, 28), Frequency.ofYears(2), STUB_NONE, EOM, BDA, null, null, null,
            list(date(2018, 2, 28), date(2020, 2, 29), date(2022, 2, 28), date(2024, 2, 28)),
            list(date(2018, 2, 28), date(2020, 2, 28), date(2022, 2, 28), date(2024, 2, 28)), EOM},
        // EOM flag true and is EOM, but end date equals start day rather than EOM
        {date(2018, 4, 30), date(2018, 10, 30), P2M, STUB_NONE, EOM, BDA, null, null, null,
            list(date(2018, 4, 30), date(2018, 6, 30), date(2018, 8, 31), date(2018, 10, 30)),
            list(date(2018, 4, 30), date(2018, 6, 29), date(2018, 8, 31), date(2018, 10, 30)), EOM},

        // pre-adjusted start date, no change needed
        {JUL_17, OCT_17, P1M, null, DAY_17, BDA, null, null, BDA_NONE,
            list(JUL_17, AUG_17, SEP_17, OCT_17),
            list(JUL_17, AUG_18, SEP_17, OCT_17), DAY_17},
        // pre-adjusted start date, change needed
        {AUG_18, OCT_17, P1M, null, DAY_17, BDA, null, null, BDA_NONE,
            list(AUG_17, SEP_17, OCT_17),
            list(AUG_18, SEP_17, OCT_17), DAY_17},
        // pre-adjusted first regular, change needed
        {JUL_11, OCT_17, P1M, null, DAY_17, BDA, AUG_18, null, BDA_NONE,
            list(JUL_11, AUG_17, SEP_17, OCT_17),
            list(JUL_11, AUG_18, SEP_17, OCT_17), DAY_17},
        // pre-adjusted last regular, change needed
        {JUL_17, OCT_17, P1M, null, DAY_17, BDA, null, AUG_18, BDA_NONE,
            list(JUL_17, AUG_17, OCT_17),
            list(JUL_17, AUG_18, OCT_17), DAY_17},
        // pre-adjusted first+last regular, change needed
        {APR_01, OCT_17, P1M, null, DAY_17, BDA, MAY_19, AUG_18, BDA_NONE,
            list(APR_01, MAY_17, JUN_17, JUL_17, AUG_17, OCT_17),
            list(APR_01, MAY_19, JUN_17, JUL_17, AUG_18, OCT_17), DAY_17},
        // pre-adjusted end date, change needed
        {JUL_17, AUG_18, P1M, null, DAY_17, BDA, null, null, BDA_NONE,
            list(JUL_17, AUG_17),
            list(JUL_17, AUG_18), DAY_17},
        // pre-adjusted end date, change needed, with adjustment
        {JUL_17, AUG_18, P1M, null, DAY_17, BDA, null, null, BDA,
            list(JUL_17, AUG_17),
            list(JUL_17, AUG_18), DAY_17},

        // TERM period
        {JUN_04, SEP_17, TERM, STUB_NONE, null, BDA, null, null, null,
            list(JUN_04, SEP_17),
            list(JUN_04, SEP_17), ROLL_NONE},
        // TERM period defined as a stub and no regular periods
        {JUN_04, SEP_17, P12M, SHORT_INITIAL, null, BDA, SEP_17, null, null,
            list(JUN_04, SEP_17),
            list(JUN_04, SEP_17), DAY_17},
        {JUN_04, SEP_17, P12M, SHORT_INITIAL, null, BDA, null, JUN_04, null,
            list(JUN_04, SEP_17),
            list(JUN_04, SEP_17), DAY_4},
        {date(2014, 9, 24), date(2016, 11, 24), Frequency.ofYears(2), SHORT_INITIAL, null, BDA, null, null, null,
            list(date(2014, 9, 24), date(2014, 11, 24), date(2016, 11, 24)),
            list(date(2014, 9, 24), date(2014, 11, 24), date(2016, 11, 24)), DAY_24},

        // IMM
        {date(2014, 9, 17), date(2014, 10, 15), P1M, STUB_NONE, IMM, BDA, null, null, null,
            list(date(2014, 9, 17), date(2014, 10, 15)),
            list(date(2014, 9, 17), date(2014, 10, 15)), IMM},
        {date(2014, 9, 17), date(2014, 10, 15), TERM, STUB_NONE, IMM, BDA, null, null, null,
            list(date(2014, 9, 17), date(2014, 10, 15)),
            list(date(2014, 9, 17), date(2014, 10, 15)), IMM},
        // IMM with stupid short period still works
        {date(2014, 9, 17), date(2014, 10, 15), Frequency.ofDays(2), STUB_NONE, IMM, BDA, null, null, null,
            list(date(2014, 9, 17), date(2014, 10, 15)),
            list(date(2014, 9, 17), date(2014, 10, 15)), IMM},
        {date(2014, 9, 17), date(2014, 10, 1), Frequency.ofDays(2), STUB_NONE, IMM, BDA, null, null, null,
            list(date(2014, 9, 17), date(2014, 10, 1)),
            list(date(2014, 9, 17), date(2014, 10, 1)), IMM},

        //IMM with adjusted start dates and various conventions
        //MF, no stub 
        {date(2018, 3, 22), date(2020, 3, 18), P6M, STUB_NONE, IMM, BDA_JPY_MF, null, null, BDA_NONE,
            list(date(2018, 3, 21), date(2018, 9, 19), date(2019, 3, 20), date(2019, 9, 18), date(2020, 3, 18)),
            list(date(2018, 3, 22), date(2018, 9, 19), date(2019, 3, 20), date(2019, 9, 18), date(2020, 3, 18)), IMM},
        //Preceding, no stub
        {date(2018, 3, 20), date(2019, 3, 20), P6M, STUB_NONE, IMM, BDA_JPY_P, null, null, BDA_NONE,
            list(date(2018, 3, 21), date(2018, 9, 19), date(2019, 3, 20)),
            list(date(2018, 3, 20), date(2018, 9, 19), date(2019, 3, 20)), IMM},
        //MF, null stub
        {date(2018, 3, 22), date(2019, 3, 20), P6M, null, IMM, BDA_JPY_MF, null, null, BDA_NONE,
            list(date(2018, 3, 21), date(2018, 9, 19), date(2019, 3, 20)),
            list(date(2018, 3, 22), date(2018, 9, 19), date(2019, 3, 20)), IMM},
        //Explicit long front stub with (adjusted) first regular start date
        {date(2017, 9, 2), date(2018, 9, 19), P6M, LONG_INITIAL, IMM, BDA_JPY_MF, date(2018, 3, 22), null, BDA_NONE,
            list(date(2017, 9, 2), date(2018, 3, 21), date(2018, 9, 19)),
            list(date(2017, 9, 2), date(2018, 3, 22), date(2018, 9, 19)), IMM},
        //Implicit short front stub with (adjusted) first regular start date
        {date(2018, 1, 2), date(2018, 9, 19), P6M, null, IMM, BDA_JPY_MF, date(2018, 3, 22), null, BDA_NONE,
            list(date(2018, 1, 2), date(2018, 3, 21), date(2018, 9, 19)),
            list(date(2018, 1, 2), date(2018, 3, 22), date(2018, 9, 19)), IMM},
        //Implicit back stub with (adjusted) last regular start date
        {date(2017, 3, 15), date(2018, 5, 19), P6M, null, IMM, BDA_JPY_MF, null, date(2018, 3, 22), BDA_NONE,
            list(date(2017, 3, 15), date(2017, 9, 20), date(2018, 3, 21), date(2018, 5, 19)),
            list(date(2017, 3, 15), date(2017, 9, 20), date(2018, 3, 22), date(2018, 5, 21)), IMM},

        // Day30 rolling with February
        {date(2015, 1, 30), date(2015, 4, 30), P1M, STUB_NONE, DAY_30, BDA, null, null, null,
            list(date(2015, 1, 30), date(2015, 2, 28), date(2015, 3, 30), date(2015, 4, 30)),
            list(date(2015, 1, 30), date(2015, 2, 27), date(2015, 3, 30), date(2015, 4, 30)), DAY_30},
        {date(2015, 2, 28), date(2015, 4, 30), P1M, STUB_NONE, DAY_30, BDA, null, null, null,
            list(date(2015, 2, 28), date(2015, 3, 30), date(2015, 4, 30)),
            list(date(2015, 2, 27), date(2015, 3, 30), date(2015, 4, 30)), DAY_30},
        {date(2015, 2, 28), date(2015, 4, 30), P1M, SHORT_INITIAL, DAY_30, BDA, null, null, null,
            list(date(2015, 2, 28), date(2015, 3, 30), date(2015, 4, 30)),
            list(date(2015, 2, 27), date(2015, 3, 30), date(2015, 4, 30)), DAY_30},

        // Two stubs no regular
        {date(2019, 1, 16), date(2020, 10, 22), P12M, null, DAY_22, BDA, date(2020, 1, 22), date(2020, 1, 22), null,
            list(date(2019, 1, 16), date(2020, 1, 22), date(2020, 10, 22)),
            list(date(2019, 1, 16), date(2020, 1, 22), date(2020, 10, 22)), DAY_22},
        {date(2019, 1, 16), date(2020, 10, 22), P12M, STUB_BOTH, DAY_22, BDA, date(2020, 1, 22), date(2020, 1, 22),
            null,
            list(date(2019, 1, 16), date(2020, 1, 22), date(2020, 10, 22)),
            list(date(2019, 1, 16), date(2020, 1, 22), date(2020, 10, 22)), DAY_22},
    };
  }

  @ParameterizedTest
  @MethodSource("data_generation")
  public void test_monthly_schedule(
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment businessDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      List<LocalDate> adjusted,
      RollConvention expRoll) {

    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .startDateBusinessDayAdjustment(startBusDayAdjustment)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .build();
    Schedule test = defn.createSchedule(REF_DATA);
    assertThat(test.size()).isEqualTo(unadjusted.size() - 1);
    for (int i = 0; i < test.size(); i++) {
      SchedulePeriod period = test.getPeriod(i);
      assertThat(period.getUnadjustedStartDate()).isEqualTo(unadjusted.get(i));
      assertThat(period.getUnadjustedEndDate()).isEqualTo(unadjusted.get(i + 1));
      assertThat(period.getStartDate()).isEqualTo(adjusted.get(i));
      assertThat(period.getEndDate()).isEqualTo(adjusted.get(i + 1));
    }
    assertThat(test.getFrequency()).isEqualTo(freq);
    assertThat(test.getRollConvention()).isEqualTo(expRoll);
  }

  @ParameterizedTest
  @MethodSource("data_generation")
  public void test_monthly_schedule_withOverride(
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment businessDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      List<LocalDate> adjusted,
      RollConvention expRoll) {

    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .startDateBusinessDayAdjustment(startBusDayAdjustment)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .overrideStartDate(AdjustableDate.of(date(2011, 1, 9), BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .build();
    Schedule test = defn.createSchedule(REF_DATA);
    assertThat(test.size()).isEqualTo(unadjusted.size() - 1);
    SchedulePeriod period0 = test.getPeriod(0);
    assertThat(period0.getUnadjustedStartDate()).isEqualTo(date(2011, 1, 9));
    assertThat(period0.getUnadjustedEndDate()).isEqualTo(unadjusted.get(1));
    assertThat(period0.getStartDate()).isEqualTo(date(2011, 1, 10));
    assertThat(period0.getEndDate()).isEqualTo(adjusted.get(1));
    for (int i = 1; i < test.size(); i++) {
      SchedulePeriod period = test.getPeriod(i);
      assertThat(period.getUnadjustedStartDate()).isEqualTo(unadjusted.get(i));
      assertThat(period.getUnadjustedEndDate()).isEqualTo(unadjusted.get(i + 1));
      assertThat(period.getStartDate()).isEqualTo(adjusted.get(i));
      assertThat(period.getEndDate()).isEqualTo(adjusted.get(i + 1));
    }
    assertThat(test.getFrequency()).isEqualTo(freq);
    assertThat(test.getRollConvention()).isEqualTo(expRoll);
  }

  @ParameterizedTest
  @MethodSource("data_generation")
  public void test_monthly_unadjusted(
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment businessDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      List<LocalDate> adjusted,
      RollConvention expRoll) {

    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .startDateBusinessDayAdjustment(startBusDayAdjustment)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .build();
    ImmutableList<LocalDate> test = defn.createUnadjustedDates(REF_DATA);
    assertThat(test).isEqualTo(unadjusted);
    // createUnadjustedDates() does not work as expected without ReferenceData
    if (startBusDayAdjustment == null && !EOM.equals(rollConv)) {
      ImmutableList<LocalDate> testNoRefData = defn.createUnadjustedDates();
      assertThat(testNoRefData).isEqualTo(unadjusted);
    }
  }

  @ParameterizedTest
  @MethodSource("data_generation")
  public void test_monthly_unadjusted_withOverride(
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment businessDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      List<LocalDate> adjusted,
      RollConvention expRoll) {

    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .startDateBusinessDayAdjustment(startBusDayAdjustment)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .overrideStartDate(AdjustableDate.of(date(2011, 1, 9), BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .build();
    ImmutableList<LocalDate> test = defn.createUnadjustedDates(REF_DATA);
    assertThat(test.get(0)).isEqualTo(date(2011, 1, 9));
    assertThat(test.subList(1, test.size())).isEqualTo(unadjusted.subList(1, test.size()));
    // createUnadjustedDates() does not work as expected without ReferenceData
    if (startBusDayAdjustment == null && !EOM.equals(rollConv)) {
      ImmutableList<LocalDate> testNoRefData = defn.createUnadjustedDates();
      assertThat(testNoRefData.get(0)).isEqualTo(date(2011, 1, 9));
      assertThat(testNoRefData.subList(1, testNoRefData.size())).isEqualTo(unadjusted.subList(1, testNoRefData.size()));
    }
  }

  @ParameterizedTest
  @MethodSource("data_generation")
  public void test_monthly_adjusted(
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment businessDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      List<LocalDate> adjusted,
      RollConvention expRoll) {

    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .startDateBusinessDayAdjustment(startBusDayAdjustment)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .build();
    ImmutableList<LocalDate> test = defn.createAdjustedDates(REF_DATA);
    assertThat(test).isEqualTo(adjusted);
  }

  @ParameterizedTest
  @MethodSource("data_generation")
  public void test_monthly_adjusted_withOverride(
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment businessDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      List<LocalDate> adjusted,
      RollConvention expRoll) {

    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .startDateBusinessDayAdjustment(startBusDayAdjustment)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .overrideStartDate(AdjustableDate.of(date(2011, 1, 9), BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .build();
    ImmutableList<LocalDate> test = defn.createAdjustedDates(REF_DATA);
    assertThat(test.get(0)).isEqualTo(date(2011, 1, 10));
    assertThat(test.subList(1, test.size())).isEqualTo(adjusted.subList(1, test.size()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_override_fallbackWhenStartDateMismatch() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUL_04)
        .endDate(SEP_17)
        .overrideStartDate(AdjustableDate.of(JUN_17, BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .rollConvention(DAY_17)
        .build();
    Schedule test = defn.createSchedule(REF_DATA);
    assertThat(test.size()).isEqualTo(3);
    SchedulePeriod period0 = test.getPeriod(0);
    assertThat(period0.getUnadjustedStartDate()).isEqualTo(JUN_17);
    assertThat(period0.getUnadjustedEndDate()).isEqualTo(JUL_17);
    assertThat(period0.getStartDate()).isEqualTo(JUN_17);
    assertThat(period0.getEndDate()).isEqualTo(JUL_17);
    SchedulePeriod period1 = test.getPeriod(1);
    assertThat(period1.getUnadjustedStartDate()).isEqualTo(JUL_17);
    assertThat(period1.getUnadjustedEndDate()).isEqualTo(AUG_17);
    assertThat(period1.getStartDate()).isEqualTo(JUL_17);
    assertThat(period1.getEndDate()).isEqualTo(AUG_18);
    SchedulePeriod period2 = test.getPeriod(2);
    assertThat(period2.getUnadjustedStartDate()).isEqualTo(AUG_17);
    assertThat(period2.getUnadjustedEndDate()).isEqualTo(SEP_17);
    assertThat(period2.getStartDate()).isEqualTo(AUG_18);
    assertThat(period2.getEndDate()).isEqualTo(SEP_17);
  }

  @Test
  public void test_override_fallbackWhenStartDateMismatchEndStub() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUL_04)
        .endDate(SEP_04)
        .overrideStartDate(AdjustableDate.of(JUN_17, BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .rollConvention(DAY_17)
        .lastRegularEndDate(AUG_17)
        .build();
    Schedule test = defn.createSchedule(REF_DATA);
    assertThat(test.size()).isEqualTo(3);
    SchedulePeriod period0 = test.getPeriod(0);
    assertThat(period0.getUnadjustedStartDate()).isEqualTo(JUN_17);
    assertThat(period0.getUnadjustedEndDate()).isEqualTo(JUL_17);
    assertThat(period0.getStartDate()).isEqualTo(JUN_17);
    assertThat(period0.getEndDate()).isEqualTo(JUL_17);
    SchedulePeriod period1 = test.getPeriod(1);
    assertThat(period1.getUnadjustedStartDate()).isEqualTo(JUL_17);
    assertThat(period1.getUnadjustedEndDate()).isEqualTo(AUG_17);
    assertThat(period1.getStartDate()).isEqualTo(JUL_17);
    assertThat(period1.getEndDate()).isEqualTo(AUG_18);
    SchedulePeriod period2 = test.getPeriod(2);
    assertThat(period2.getUnadjustedStartDate()).isEqualTo(AUG_17);
    assertThat(period2.getUnadjustedEndDate()).isEqualTo(SEP_04);
    assertThat(period2.getStartDate()).isEqualTo(AUG_18);
    assertThat(period2.getEndDate()).isEqualTo(SEP_04);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_startEndAdjust() {
    BusinessDayAdjustment bda1 = BusinessDayAdjustment.of(PRECEDING, SAT_SUN);
    BusinessDayAdjustment bda2 = BusinessDayAdjustment.of(MODIFIED_PRECEDING, SAT_SUN);
    PeriodicSchedule test = PeriodicSchedule.builder()
        .startDate(date(2014, 10, 4))
        .endDate(date(2015, 4, 4))
        .frequency(P3M)
        .businessDayAdjustment(BDA)
        .startDateBusinessDayAdjustment(bda1)
        .endDateBusinessDayAdjustment(bda2)
        .stubConvention(STUB_NONE)
        .build();
    assertThat(test.calculatedStartDate()).isEqualTo(AdjustableDate.of(date(2014, 10, 4), bda1));
    assertThat(test.calculatedEndDate()).isEqualTo(AdjustableDate.of(date(2015, 4, 4), bda2));
    assertThat(test.createUnadjustedDates()).containsExactly(date(2014, 10, 4), date(2015, 1, 4), date(2015, 4, 4));
    assertThat(test.createAdjustedDates(REF_DATA))
        .containsExactly(date(2014, 10, 3), date(2015, 1, 5), date(2015, 4, 3));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_replace() {
    return new Object[][] {
        // SmartInitial is set
        {JUN_11, JUN_17, AUG_17, P1M, null, DAY_17, BDA, null, null, BDA_JPY_P,
            list(JUN_11, JUL_17, AUG_17), SMART_INITIAL, null, DAY_17},
        // SmartInitial not set
        {MAY_19, JUN_17, AUG_17, P1M, LONG_INITIAL, DAY_17, BDA, JUN_17, AUG_17, BDA_JPY_P,
            list(MAY_19, JUL_17, AUG_17), LONG_INITIAL, AUG_17, DAY_17},
        // start set to be later
        {JUL_04, JUN_17, AUG_17, P1M, null, DAY_17, BDA, JUN_17, AUG_17, BDA_JPY_P,
            list(JUL_04, JUL_17, AUG_17), SMART_INITIAL, AUG_17, DAY_17},
        // original schedule had no stubs and NONE, new schedule uses SmartInitial instead
        {JUN_04, JUN_17, AUG_17, P1M, STUB_NONE, null, BDA, null, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17), SMART_INITIAL, null, null},
        // original schedule had double stubs with stub convention and first regular date to determine roll of 17th
        // new schedule uses SmartInitial and calculated last regular
        {JUN_04, JUN_03, AUG_30, P1M, SMART_FINAL, null, BDA, JUN_17, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, AUG_30), SMART_INITIAL, AUG_17, null},
        // original schedule had double explicit stubs, new schedule uses SmartInitial instead of first regular
        {JUN_04, JUN_03, AUG_30, P1M, null, null, BDA, JUN_17, AUG_17, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, AUG_30), SMART_INITIAL, AUG_17, null},
        // original schedule had double explicit stubs and BOTH, new schedule uses SmartInitial instead of first regular
        {JUN_04, JUN_03, AUG_30, P1M, STUB_BOTH, null, BDA, JUN_17, AUG_17, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17, AUG_30), SMART_INITIAL, AUG_17, null},
        // original schedule had first regular date, new schedule just uses SmartInitial
        {JUN_04, JUN_03, AUG_17, P1M, null, null, BDA, JUN_17, null, null,
            list(JUN_04, JUN_17, JUL_17, AUG_17), SMART_INITIAL, null, null},
        // original schedule had last regular date and uneccessary final stub convention
        {JUN_04, JUN_17, AUG_04, P1M, SHORT_FINAL, null, BDA, null, JUL_17, null,
            list(JUN_04, JUN_17, JUL_17, AUG_04), SMART_INITIAL, JUL_17, null},
        // original schedule was final, but resulted in Term schedule, new schedule retains the stub convention
        {JUN_04, JUL_17, AUG_17, P1M, SHORT_FINAL, null, BDA, null, null, null,
            list(JUN_04, JUL_04, AUG_04, AUG_17), SHORT_FINAL, null, null},
        // cannot set start after end
        {SEP_04, JUN_17, AUG_17, P1M, null, DAY_17, BDA, JUN_17, AUG_17, BDA_JPY_P, null, null, null, null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_replace")
  public void test_replace(
      LocalDate replaceStart,
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment businessDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      StubConvention expectedStubConvention,
      LocalDate expectedLastRegular,
      RollConvention expectedRollConvention) {

    PeriodicSchedule base = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .startDateBusinessDayAdjustment(startBusDayAdjustment)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .build();
    if (unadjusted == null) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> base.replaceStartDate(replaceStart).createSchedule(REF_DATA));
    } else {
      PeriodicSchedule test = base.replaceStartDate(replaceStart);
      assertThat(test.getOverrideStartDate()).isEmpty();
      assertThat(test.getStartDate()).isEqualTo(replaceStart);
      assertThat(test.getStartDateBusinessDayAdjustment()).hasValue(BDA_NONE);
      assertThat(test.getFirstRegularStartDate()).isEmpty();
      assertThat(test.getBusinessDayAdjustment()).isEqualTo(businessDayAdjustment);
      assertThat(test.getLastRegularEndDate()).isEqualTo(Optional.ofNullable(expectedLastRegular));
      assertThat(test.getEndDate()).isEqualTo(end);
      assertThat(test.getEndDateBusinessDayAdjustment()).isEmpty();
      assertThat(test.getStubConvention()).isEqualTo(Optional.ofNullable(expectedStubConvention));
      assertThat(test.getRollConvention()).isEqualTo(Optional.ofNullable(expectedRollConvention));
      assertThat(test.createUnadjustedDates()).isEqualTo(unadjusted);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_none_badStub() {
    // Jun 4th to Sep 17th requires a stub, but NONE specified
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUN_04)
        .endDate(SEP_17)
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(DAY_4)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> defn.createUnadjustedDates());
  }

  @Test
  public void test_none_stubDate() {
    // Jun 17th to Sep 17th is correct for NONE stub convention, but firstRegularStartDate specified
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUN_17)
        .endDate(SEP_17)
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(DAY_4)
        .firstRegularStartDate(JUL_17)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> defn.createUnadjustedDates());
  }

  @Test
  public void test_both_badStub() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUN_17)
        .endDate(SEP_17)
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_BOTH)
        .rollConvention(null)
        .firstRegularStartDate(JUN_17)
        .lastRegularEndDate(SEP_17)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> defn.createUnadjustedDates());
  }

  @Test
  public void test_backwards_badStub() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUN_17)
        .endDate(SEP_17)
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .stubConvention(SHORT_INITIAL)
        .rollConvention(DAY_11)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> defn.createUnadjustedDates());
  }

  @Test
  public void test_forwards_badStub() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUN_17)
        .endDate(SEP_17)
        .frequency(P1M)
        .businessDayAdjustment(BDA)
        .stubConvention(SHORT_FINAL)
        .rollConvention(DAY_11)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> defn.createUnadjustedDates());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_termFrequency_badInitialStub() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUN_04)
        .endDate(SEP_17)
        .frequency(TERM)
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(DAY_4)
        .firstRegularStartDate(JUN_17)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> defn.createUnadjustedDates());
  }

  @Test
  public void test_termFrequency_badFinalStub() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(JUN_04)
        .endDate(SEP_17)
        .frequency(TERM)
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(DAY_4)
        .firstRegularStartDate(null)
        .lastRegularEndDate(SEP_04)
        .build();
    assertThatExceptionOfType(ScheduleException.class).isThrownBy(() -> defn.createUnadjustedDates());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_emptyWhenAdjusted_term_createUnadjustedDates() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 29))
        .endDate(date(2015, 5, 31))
        .frequency(TERM)
        .businessDayAdjustment(BDA)
        .stubConvention(null)
        .rollConvention(null)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    ImmutableList<LocalDate> test = defn.createUnadjustedDates();
    assertThat(test).containsExactly(date(2015, 5, 29), date(2015, 5, 31));
  }

  @Test
  public void test_emptyWhenAdjusted_term_createAdjustedDates() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 29))
        .endDate(date(2015, 5, 31))
        .frequency(TERM)
        .businessDayAdjustment(BDA)
        .stubConvention(null)
        .rollConvention(null)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class)
        .isThrownBy(() -> defn.createAdjustedDates(REF_DATA))
        .withMessageMatching(".*duplicate adjusted dates.*");
  }

  @Test
  public void test_emptyWhenAdjusted_term_createSchedule() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 29))
        .endDate(date(2015, 5, 31))
        .frequency(TERM)
        .businessDayAdjustment(BDA)
        .stubConvention(null)
        .rollConvention(null)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class)
        .isThrownBy(() -> defn.createSchedule(REF_DATA))
        .withMessageMatching(".*duplicate adjusted dates.*");
  }

  @Test
  public void test_combinePeriodsWhenNecessary_1w_createSchedule() {
    HolidayCalendarId id = HolidayCalendarId.of("calendar");
    HolidayCalendar calendar = new HolidayCalendar() {
      private Set<LocalDate> holidays = IntStream.range(1, 9)
          .mapToObj(day -> date(2020, 10, day))
          .collect(toImmutableSet());

      @Override
      public boolean isHoliday(LocalDate date) {
        return HolidayCalendars.SAT_SUN.isHoliday(date) || holidays.contains(date);
      }

      @Override
      public HolidayCalendarId getId() {
        return id;
      }
    };

    ReferenceData referenceData = ImmutableReferenceData.of(id, calendar);
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, id);
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2020, 9, 18))
        .endDate(date(2020, 12, 18))
        .frequency(P1W)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(SHORT_FINAL)
        .rollConvention(null)
        .build();

    Schedule schedule = defn.createSchedule(referenceData, true);
    assertThat(schedule.getPeriods()).hasSize(12);
    assertThat(schedule.getPeriod(1).getStartDate()).isEqualTo(date(2020, 9, 25));
    assertThat(schedule.getPeriod(2).getStartDate()).isEqualTo(date(2020, 10, 9));
  }

  @Test
  public void test_combinePeriodsWhenNecessary_1d_createSchedule_duplicate_exception() {
    HolidayCalendarId id = SAT_SUN;
    HolidayCalendar calendar = HolidayCalendars.SAT_SUN;

    ReferenceData referenceData = ImmutableReferenceData.of(id, calendar);
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, id);
    PeriodicSchedule defn = PeriodicSchedule.builder()
            .startDate(date(2020, 9, 18))
            .endDate(date(2020, 12, 18))
            .frequency(P1D)
            .businessDayAdjustment(businessDayAdjustment)
            .stubConvention(SHORT_FINAL)
            .rollConvention(null)
            .build();

    Schedule schedule = defn.createSchedule(referenceData, true);
    assertThat(schedule.getPeriods()).hasSize(65);
    assertThat(schedule.getPeriod(0).getStartDate()).isEqualTo(date(2020, 9, 18));
    assertThat(schedule.getPeriod(0).getEndDate()).isEqualTo(date(2020, 9, 21));
  }

  @Test
  public void test_combinePeriodsWhenNecessary_1d_createSchedule() {
    HolidayCalendarId id = SAT_SUN;
    HolidayCalendar calendar = HolidayCalendars.SAT_SUN;

    ReferenceData referenceData = ImmutableReferenceData.of(id, calendar);
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, id);
    PeriodicSchedule defn = PeriodicSchedule.builder()
            .startDate(date(2020, 9, 18))
            .endDate(date(2020, 12, 18))
            .frequency(P1D)
            .businessDayAdjustment(businessDayAdjustment)
            .stubConvention(SHORT_FINAL)
            .rollConvention(null)
            .build();

    assertThatExceptionOfType(ScheduleException.class)
            .isThrownBy(() -> defn.createSchedule(referenceData, false))
            .withMessageMatching(".*duplicate adjusted dates.*");
  }

  @Test
  public void test_emptyWhenAdjusted_twoPeriods_createUnadjustedDates() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 27))
        .endDate(date(2015, 5, 31))
        .frequency(Frequency.ofDays(2))
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(null)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    ImmutableList<LocalDate> test = defn.createUnadjustedDates();
    assertThat(test).containsExactly(date(2015, 5, 27), date(2015, 5, 29), date(2015, 5, 31));
  }

  @Test
  public void test_emptyWhenAdjusted_twoPeriods_createAdjustedDates() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 27))
        .endDate(date(2015, 5, 31))
        .frequency(Frequency.ofDays(2))
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(null)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class)
        .isThrownBy(() -> defn.createAdjustedDates(REF_DATA))
        .withMessageMatching(".*duplicate adjusted dates.*");
  }

  @Test
  public void test_emptyWhenAdjusted_twoPeriods_createSchedule() {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 27))
        .endDate(date(2015, 5, 31))
        .frequency(Frequency.ofDays(2))
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(null)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class)
        .isThrownBy(() -> defn.createSchedule(REF_DATA))
        .withMessageMatching(".*duplicate adjusted dates.*");
  }

  @Test
  public void test_brokenWhenAdjusted_twoPeriods_createSchedule() {
    // generate unadjusted dates that are sorted (Wed, then Fri, then Sun)
    // use weird BusinessDayConvention to move Sunday back to Thursday
    // result is adjusted dates that are not sorted (Wed, then Fri, then Thu)
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 27))
        .endDate(date(2015, 5, 31))
        .frequency(Frequency.ofDays(2))
        .businessDayAdjustment(BusinessDayAdjustment.of(new BusinessDayConvention() {
          @Override
          public String getName() {
            return "TestBack3OnSun";
          }

          @Override
          public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
            return (date.getDayOfWeek() == SUNDAY ? date.minusDays(3) : date);
          }
        }, NO_HOLIDAYS))
        .stubConvention(STUB_NONE)
        .rollConvention(null)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class)
        .isThrownBy(() -> defn.createSchedule(REF_DATA))
        .withMessage("Schedule calculation resulted in invalid period");
  }

  @Test
  public void test_emptyWhenAdjusted_badRoll_createUnadjustedDates() {
    RollConvention roll = new RollConvention() {
      private boolean seen;

      @Override
      public String getName() {
        return "Test";
      }

      @Override
      public LocalDate adjust(LocalDate date) {
        return date;
      }

      @Override
      public LocalDate next(LocalDate date, Frequency frequency) {
        if (seen) {
          return date.plus(frequency);
        } else {
          seen = true;
          return date;
        }
      }
    };
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(date(2015, 5, 27))
        .endDate(date(2015, 5, 31))
        .frequency(Frequency.ofDays(2))
        .businessDayAdjustment(BDA)
        .stubConvention(STUB_NONE)
        .rollConvention(roll)
        .firstRegularStartDate(null)
        .lastRegularEndDate(null)
        .build();
    assertThatExceptionOfType(ScheduleException.class)
        .isThrownBy(() -> defn.createUnadjustedDates())
        .withMessageMatching(".*duplicate unadjusted dates.*");
  }

  //-------------------------------------------------------------------------
  @ParameterizedTest
  @MethodSource("data_generation")
  public void coverage_equals(
      LocalDate start,
      LocalDate end,
      Frequency freq,
      StubConvention stubConv,
      RollConvention rollConv,
      BusinessDayAdjustment busDayAdjustment,
      LocalDate firstReg,
      LocalDate lastReg,
      BusinessDayAdjustment startBusDayAdjustment,
      List<LocalDate> unadjusted,
      List<LocalDate> adjusted,
      RollConvention expRoll) {

    PeriodicSchedule a1 = of(start, end, freq, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule a2 = of(start, end, freq, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule b = of(LocalDate.MIN, end, freq, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule c = of(start, LocalDate.MAX, freq, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule d = of(
        start, end, freq == P1M ? P3M : P1M, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule e = of(
        start, end, freq, BDA_NONE, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule f = of(
        start, end, freq, busDayAdjustment, stubConv == STUB_NONE ? SHORT_FINAL : STUB_NONE, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule g = of(start, end, freq, busDayAdjustment, stubConv, SFE, firstReg, lastReg, null, null, null);
    PeriodicSchedule h = of(start, end, freq, busDayAdjustment, stubConv, rollConv, start.plusDays(1), null, null, null, null);
    PeriodicSchedule i = of(start, end, freq, busDayAdjustment, stubConv, rollConv, null, end.minusDays(1), null, null, null);
    PeriodicSchedule j = of(start, end, freq, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, BDA, null, null);
    PeriodicSchedule k = of(start, end, freq, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, null, BDA, null);
    PeriodicSchedule m = of(
        start, end, freq, busDayAdjustment, stubConv, rollConv, firstReg, lastReg, null, null, AdjustableDate.of(start.minusDays(1)));
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
    assertThat(a1.equals(d)).isEqualTo(false);
    assertThat(a1.equals(e)).isEqualTo(false);
    assertThat(a1.equals(f)).isEqualTo(false);
    assertThat(a1.equals(g)).isEqualTo(false);
    assertThat(a1.equals(h)).isEqualTo(false);
    assertThat(a1.equals(i)).isEqualTo(false);
    assertThat(a1.equals(j)).isEqualTo(false);
    assertThat(a1.equals(k)).isEqualTo(false);
    assertThat(a1.equals(m)).isEqualTo(false);
  }

  private PeriodicSchedule of(
      LocalDate start, LocalDate end, Frequency freq, BusinessDayAdjustment bda,
      StubConvention stubConv, RollConvention rollConv, LocalDate firstReg, LocalDate lastReg,
      BusinessDayAdjustment startBda, BusinessDayAdjustment endBda, AdjustableDate overrideStartDate) {
    return PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .businessDayAdjustment(bda)
        .startDateBusinessDayAdjustment(startBda)
        .endDateBusinessDayAdjustment(endBda)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .overrideStartDate(overrideStartDate)
        .build();
  }

  @Test
  public void coverage_builder() {
    PeriodicSchedule test = PeriodicSchedule.builder()
        .startDate(JUL_17)
        .endDate(SEP_17)
        .frequency(P2M)
        .businessDayAdjustment(BDA_NONE)
        .startDateBusinessDayAdjustment(BDA_NONE)
        .endDateBusinessDayAdjustment(BDA_NONE)
        .stubConvention(STUB_NONE)
        .rollConvention(EOM)
        .firstRegularStartDate(JUL_17)
        .lastRegularEndDate(SEP_17)
        .overrideStartDate(AdjustableDate.of(JUL_11))
        .build();
    assertThat(test.getStartDate()).isEqualTo(JUL_17);
    assertThat(test.getEndDate()).isEqualTo(SEP_17);
    assertThat(test.calculatedStartDate()).isEqualTo(AdjustableDate.of(JUL_11, BDA_NONE));
    assertThat(test.calculatedEndDate()).isEqualTo(AdjustableDate.of(SEP_17, BDA_NONE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);
    PeriodicSchedule defn = PeriodicSchedule.of(
        date(2014, JUNE, 4),
        date(2014, SEPTEMBER, 17),
        P1M,
        bda,
        SHORT_INITIAL,
        false);
    coverImmutableBean(defn);
  }

  @Test
  public void test_serialization() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, SAT_SUN);
    PeriodicSchedule defn = PeriodicSchedule.of(
        date(2014, JUNE, 4),
        date(2014, SEPTEMBER, 17),
        P1M,
        bda,
        SHORT_INITIAL,
        false);
    assertSerialization(defn);
  }

}
