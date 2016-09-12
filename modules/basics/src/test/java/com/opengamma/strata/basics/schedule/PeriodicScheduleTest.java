/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_PRECEDING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_11;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_17;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_4;
import static com.opengamma.strata.basics.schedule.RollConventions.EOM;
import static com.opengamma.strata.basics.schedule.RollConventions.IMM;
import static com.opengamma.strata.basics.schedule.RollConventions.SFE;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_INITIAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.Month.AUGUST;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static java.time.Month.NOVEMBER;
import static java.time.Month.SEPTEMBER;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.HolidayCalendar;

/**
 * Test {@link PeriodicSchedule}.
 */
@Test
public class PeriodicScheduleTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final StubConvention STUB_NONE = StubConvention.NONE;
  private static final StubConvention STUB_BOTH = StubConvention.BOTH;
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
  private static final LocalDate NOV_30_2013 = date(2013, NOVEMBER, 30);
  private static final LocalDate FEB_28 = date(2014, FEBRUARY, 28);
  private static final LocalDate MAY_30 = date(2014, MAY, 30);
  private static final LocalDate MAY_31 = date(2014, MAY, 31);
  private static final LocalDate AUG_30 = date(2014, AUGUST, 30);
  private static final LocalDate AUG_31 = date(2014, AUGUST, 31);
  private static final LocalDate NOV_30 = date(2014, NOVEMBER, 30);
  private static final LocalDate JUN_03 = date(2014, JUNE, 3);
  private static final LocalDate JUN_04 = date(2014, JUNE, 4);
  private static final LocalDate JUN_17 = date(2014, JUNE, 17);
  private static final LocalDate JUL_04 = date(2014, JULY, 4);
  private static final LocalDate JUL_11 = date(2014, JULY, 11);
  private static final LocalDate JUL_17 = date(2014, JULY, 17);
  private static final LocalDate AUG_04 = date(2014, AUGUST, 4);
  private static final LocalDate AUG_11 = date(2014, AUGUST, 11);
  private static final LocalDate AUG_17 = date(2014, AUGUST, 17);
  private static final LocalDate AUG_18 = date(2014, AUGUST, 18);
  private static final LocalDate SEP_04 = date(2014, SEPTEMBER, 4);
  private static final LocalDate SEP_05 = date(2014, SEPTEMBER, 5);
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17);
  private static final LocalDate SEP_18 = date(2014, SEPTEMBER, 18);

  //-------------------------------------------------------------------------
  public void test_of_LocalDateEomFalse() {
    PeriodicSchedule test = PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, false);
    assertEquals(test.getStartDate(), JUN_04);
    assertEquals(test.getEndDate(), SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStartDateBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getEndDateBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getStubConvention(), Optional.of(SHORT_INITIAL));
    assertEquals(test.getRollConvention(), Optional.empty());
    assertEquals(test.getFirstRegularStartDate(), Optional.empty());
    assertEquals(test.getLastRegularEndDate(), Optional.empty());
    assertEquals(test.getOverrideStartDate(), Optional.empty());
    assertEquals(test.calculatedRollConvention(), DAY_17);
    assertEquals(test.calculatedFirstRegularStartDate(), JUN_04);
    assertEquals(test.calculatedLastRegularEndDate(), SEP_17);
    assertEquals(test.calculatedStartDate(), AdjustableDate.of(JUN_04, BDA));
    assertEquals(test.calculatedEndDate(), AdjustableDate.of(SEP_17, BDA));
  }

  public void test_of_LocalDateEomTrue() {
    PeriodicSchedule test = PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_FINAL, true);
    assertEquals(test.getStartDate(), JUN_04);
    assertEquals(test.getEndDate(), SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStartDateBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getEndDateBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getStubConvention(), Optional.of(SHORT_FINAL));
    assertEquals(test.getRollConvention(), Optional.of(EOM));
    assertEquals(test.getFirstRegularStartDate(), Optional.empty());
    assertEquals(test.getLastRegularEndDate(), Optional.empty());
    assertEquals(test.getOverrideStartDate(), Optional.empty());
    assertEquals(test.calculatedRollConvention(), DAY_4);
    assertEquals(test.calculatedFirstRegularStartDate(), JUN_04);
    assertEquals(test.calculatedLastRegularEndDate(), SEP_17);
    assertEquals(test.calculatedStartDate(), AdjustableDate.of(JUN_04, BDA));
    assertEquals(test.calculatedEndDate(), AdjustableDate.of(SEP_17, BDA));
  }

  public void test_of_LocalDateEom_null() {
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(null, SEP_17, P1M, BDA, SHORT_INITIAL, false));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, null, P1M, BDA, SHORT_INITIAL, false));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, SEP_17, null, BDA, SHORT_INITIAL, false));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, null, SHORT_INITIAL, false));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, null, false));
  }

  //-------------------------------------------------------------------------
  public void test_of_LocalDateRoll() {
    PeriodicSchedule test = PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17);
    assertEquals(test.getStartDate(), JUN_04);
    assertEquals(test.getEndDate(), SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStartDateBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getEndDateBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getStubConvention(), Optional.of(SHORT_INITIAL));
    assertEquals(test.getRollConvention(), Optional.of(DAY_17));
    assertEquals(test.getFirstRegularStartDate(), Optional.empty());
    assertEquals(test.getLastRegularEndDate(), Optional.empty());
    assertEquals(test.getOverrideStartDate(), Optional.empty());
    assertEquals(test.calculatedRollConvention(), DAY_17);
    assertEquals(test.calculatedFirstRegularStartDate(), JUN_04);
    assertEquals(test.calculatedLastRegularEndDate(), SEP_17);
    assertEquals(test.calculatedStartDate(), AdjustableDate.of(JUN_04, BDA));
    assertEquals(test.calculatedEndDate(), AdjustableDate.of(SEP_17, BDA));
  }

  public void test_of_LocalDateRoll_null() {
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(null, SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, null, P1M, BDA, SHORT_INITIAL, DAY_17));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, SEP_17, null, BDA, SHORT_INITIAL, DAY_17));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, null, SHORT_INITIAL, DAY_17));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, null, DAY_17));
    assertThrowsIllegalArg(() -> PeriodicSchedule.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, null));
  }

  //-------------------------------------------------------------------------
  public void test_builder_invalidDateOrder() {
    // start vs end
    assertThrowsIllegalArg(() -> createDates(SEP_17, SEP_17, null, null));
    assertThrowsIllegalArg(() -> createDates(SEP_17, JUN_04, null, null));
    // first/last regular vs start/end
    assertThrowsIllegalArg(() -> createDates(JUN_04, SEP_17, JUN_03, null));
    assertThrowsIllegalArg(() -> createDates(JUN_04, SEP_17, null, SEP_18));
    // first regular vs last regular
    assertThrowsIllegalArg(() -> createDates(JUN_04, SEP_17, SEP_05, SEP_05));
    assertThrowsIllegalArg(() -> createDates(JUN_04, SEP_17, SEP_05, SEP_04));
    // first regular vs override start date
    assertThrowsIllegalArg(() -> PeriodicSchedule.builder()
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
  @DataProvider(name = "generation")
  Object[][] data_generation() {
    return new Object[][] {
        // stub null
        {JUN_17, SEP_17, P1M, null, null, null, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},

        // stub NONE
        {JUN_17, SEP_17, P1M, STUB_NONE, null, null, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_17, JUL_17, P1M, STUB_NONE, null, null, null,
            ImmutableList.of(JUN_17, JUL_17),
            ImmutableList.of(JUN_17, JUL_17)},

        // stub SHORT_INITIAL
        {JUN_04, SEP_17, P1M, SHORT_INITIAL, null, null, null,
            ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_17, SEP_17, P1M, SHORT_INITIAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_17, JUL_04, P1M, SHORT_INITIAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_04),
            ImmutableList.of(JUN_17, JUL_04)},
        {date(2011, 6, 28), date(2011, 6, 30), P1M, SHORT_INITIAL, EOM, null, null,
            ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30)),
            ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30))},
        {date(2014, 12, 12), date(2015, 8, 24), P3M, SHORT_INITIAL, null, null, null,
            ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
            ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24))},
        {date(2014, 12, 12), date(2015, 8, 24), P3M, SHORT_INITIAL, RollConventions.NONE, null, null,
            ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
            ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24))},
        {date(2014, 11, 24), date(2015, 8, 24), P3M, null, RollConventions.NONE, null, null,
            ImmutableList.of(date(2014, 11, 24), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
            ImmutableList.of(date(2014, 11, 24), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24))},

        // stub LONG_INITIAL
        {JUN_04, SEP_17, P1M, LONG_INITIAL, null, null, null,
            ImmutableList.of(JUN_04, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_04, JUL_17, AUG_18, SEP_17)},
        {JUN_17, SEP_17, P1M, LONG_INITIAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_17, JUL_04, P1M, LONG_INITIAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_04),
            ImmutableList.of(JUN_17, JUL_04)},
        {JUN_17, AUG_04, P1M, LONG_INITIAL, null, null, null,
            ImmutableList.of(JUN_17, AUG_04),
            ImmutableList.of(JUN_17, AUG_04)},

        // stub SHORT_FINAL
        {JUN_04, SEP_17, P1M, SHORT_FINAL, null, null, null,
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17),
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17)},
        {JUN_17, SEP_17, P1M, SHORT_FINAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_17, JUL_04, P1M, SHORT_FINAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_04),
            ImmutableList.of(JUN_17, JUL_04)},
        {date(2011, 6, 28), date(2011, 6, 30), P1M, SHORT_FINAL, EOM, null, null,
            ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30)),
            ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30))},
        {date(2014, 11, 29), date(2015, 9, 2), P3M, SHORT_FINAL, null, null, null,
            ImmutableList.of(date(2014, 11, 29), date(2015, 2, 28), date(2015, 5, 29), date(2015, 8, 29), date(2015, 9, 2)),
            ImmutableList.of(date(2014, 11, 28), date(2015, 2, 27), date(2015, 5, 29), date(2015, 8, 31), date(2015, 9, 2))},
        {date(2014, 11, 29), date(2015, 9, 2), P3M, SHORT_FINAL, RollConventions.NONE, null, null,
            ImmutableList.of(date(2014, 11, 29), date(2015, 2, 28), date(2015, 5, 29), date(2015, 8, 29), date(2015, 9, 2)),
            ImmutableList.of(date(2014, 11, 28), date(2015, 2, 27), date(2015, 5, 29), date(2015, 8, 31), date(2015, 9, 2))},

        // stub LONG_FINAL
        {JUN_04, SEP_17, P1M, LONG_FINAL, null, null, null,
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17),
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17)},
        {JUN_17, SEP_17, P1M, LONG_FINAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_17, JUL_04, P1M, LONG_FINAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_04),
            ImmutableList.of(JUN_17, JUL_04)},
        {JUN_17, AUG_04, P1M, LONG_FINAL, null, null, null,
            ImmutableList.of(JUN_17, AUG_04),
            ImmutableList.of(JUN_17, AUG_04)},

        // explicit initial stub
        {JUN_04, SEP_17, P1M, null, null, JUN_17, null,
            ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_04, SEP_17, P1M, SHORT_INITIAL, null, JUN_17, null,
            ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17)},
        {JUN_17, SEP_17, P1M, null, null, JUN_17, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},

        // explicit final stub
        {JUN_04, SEP_17, P1M, null, null, null, AUG_04,
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17),
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17)},
        {JUN_04, SEP_17, P1M, SHORT_FINAL, null, null, AUG_04,
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17),
            ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17)},
        {JUN_17, SEP_17, P1M, null, null, null, AUG_17,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},

        // explicit double stub
        {JUN_04, SEP_17, P1M, null, null, JUL_11, AUG_11,
            ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17),
            ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17)},
        {JUN_04, SEP_17, P1M, STUB_BOTH, null, JUL_11, AUG_11,
            ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17),
            ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17)},
        {JUN_17, SEP_17, P1M, null, null, JUN_17, SEP_17,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},

        // near end of month
        // EOM flag false, thus roll on 30th
        {NOV_30_2013, NOV_30, P3M, STUB_NONE, null, null, null,
            ImmutableList.of(NOV_30_2013, FEB_28, MAY_30, AUG_30, NOV_30),
            ImmutableList.of(date(2013, NOVEMBER, 29), FEB_28, MAY_30, date(2014, AUGUST, 29), date(2014, NOVEMBER, 28))},
        // EOM flag true and is EOM, thus roll at EOM
        {NOV_30_2013, NOV_30, P3M, STUB_NONE, EOM, null, null,
            ImmutableList.of(NOV_30_2013, FEB_28, MAY_31, AUG_31, NOV_30),
            ImmutableList.of(date(2013, NOVEMBER, 29), FEB_28, MAY_30, date(2014, AUGUST, 29), date(2014, NOVEMBER, 28))},
        // EOM flag true, but not EOM, thus roll on 30th (stub convention defined)
        {MAY_30, NOV_30, P3M, STUB_NONE, EOM, null, null,
            ImmutableList.of(MAY_30, AUG_30, NOV_30),
            ImmutableList.of(MAY_30, date(2014, AUGUST, 29), date(2014, NOVEMBER, 28))},
        // EOM flag true, but not EOM, thus roll on 30th (no stub convention defined)
        {MAY_30, NOV_30, P3M, null, EOM, null, null,
            ImmutableList.of(MAY_30, AUG_30, NOV_30),
            ImmutableList.of(MAY_30, date(2014, AUGUST, 29), date(2014, NOVEMBER, 28))},
        // EOM flag true and is EOM, double stub, thus roll at EOM
        {date(2014, 1, 3), SEP_17, P3M, STUB_BOTH, EOM, FEB_28, AUG_31,
            ImmutableList.of(date(2014, 1, 3), FEB_28, MAY_31, AUG_31, SEP_17),
            ImmutableList.of(date(2014, 1, 3), FEB_28, MAY_30, date(2014, AUGUST, 29), SEP_17)},

        // TERM period
        {JUN_04, SEP_17, TERM, STUB_NONE, null, null, null,
            ImmutableList.of(JUN_04, SEP_17),
            ImmutableList.of(JUN_04, SEP_17)},
        // TERM period defined as a stub and no regular periods
        {JUN_04, SEP_17, P12M, SHORT_INITIAL, null, SEP_17, null,
            ImmutableList.of(JUN_04, SEP_17),
            ImmutableList.of(JUN_04, SEP_17)},
        {JUN_04, SEP_17, P12M, SHORT_INITIAL, null, null, JUN_04,
            ImmutableList.of(JUN_04, SEP_17),
            ImmutableList.of(JUN_04, SEP_17)},
        {date(2014, 9, 24), date(2016, 11, 24), Frequency.ofYears(2), SHORT_INITIAL, null, null, null,
            ImmutableList.of(date(2014, 9, 24), date(2014, 11, 24), date(2016, 11, 24)),
            ImmutableList.of(date(2014, 9, 24), date(2014, 11, 24), date(2016, 11, 24))},

        // IMM
        {date(2014, 9, 17), date(2014, 10, 15), P1M, STUB_NONE, IMM, null, null,
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15)),
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15))},
        {date(2014, 9, 17), date(2014, 10, 15), TERM, STUB_NONE, IMM, null, null,
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15)),
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15))},
        // IMM with stupid short period still works
        {date(2014, 9, 17), date(2014, 10, 15), Frequency.ofDays(2), STUB_NONE, IMM, null, null,
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15)),
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15))},
        {date(2014, 9, 17), date(2014, 10, 1), Frequency.ofDays(2), STUB_NONE, IMM, null, null,
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 1)),
            ImmutableList.of(date(2014, 9, 17), date(2014, 10, 1))},

        // Day30 rolling with February
        {date(2015, 1, 30), date(2015, 4, 30), P1M, STUB_NONE, RollConvention.ofDayOfMonth(30), null, null,
            ImmutableList.of(date(2015, 1, 30), date(2015, 2, 28), date(2015, 3, 30), date(2015, 4, 30)),
            ImmutableList.of(date(2015, 1, 30), date(2015, 2, 27), date(2015, 3, 30), date(2015, 4, 30))},
        {date(2015, 2, 28), date(2015, 4, 30), P1M, STUB_NONE, RollConvention.ofDayOfMonth(30), null, null,
            ImmutableList.of(date(2015, 2, 28), date(2015, 3, 30), date(2015, 4, 30)),
            ImmutableList.of(date(2015, 2, 27), date(2015, 3, 30), date(2015, 4, 30))},
        {date(2015, 2, 28), date(2015, 4, 30), P1M, SHORT_INITIAL, RollConvention.ofDayOfMonth(30), null, null,
            ImmutableList.of(date(2015, 2, 28), date(2015, 3, 30), date(2015, 4, 30)),
            ImmutableList.of(date(2015, 2, 27), date(2015, 3, 30), date(2015, 4, 30))},
    };
  }

  @Test(dataProvider = "generation")
  public void test_monthly_schedule(
      LocalDate start, LocalDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .businessDayAdjustment(BDA)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .build();
    Schedule test = defn.createSchedule(REF_DATA);
    assertEquals(test.size(), unadjusted.size() - 1);
    for (int i = 0; i < test.size(); i++) {
      SchedulePeriod period = test.getPeriod(i);
      assertEquals(period.getUnadjustedStartDate(), unadjusted.get(i));
      assertEquals(period.getUnadjustedEndDate(), unadjusted.get(i + 1));
      assertEquals(period.getStartDate(), adjusted.get(i));
      assertEquals(period.getEndDate(), adjusted.get(i + 1));
    }
    assertEquals(test.getFrequency(), freq);
    assertEquals(test.getRollConvention(), defn.calculatedRollConvention());
  }

  @Test(dataProvider = "generation")
  public void test_monthly_schedule_withOverride(
      LocalDate start, LocalDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .businessDayAdjustment(BDA)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .overrideStartDate(AdjustableDate.of(date(2011, 1, 9), BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .build();
    Schedule test = defn.createSchedule(REF_DATA);
    assertEquals(test.size(), unadjusted.size() - 1);
    SchedulePeriod period0 = test.getPeriod(0);
    assertEquals(period0.getUnadjustedStartDate(), date(2011, 1, 9));
    assertEquals(period0.getUnadjustedEndDate(), unadjusted.get(1));
    assertEquals(period0.getStartDate(), date(2011, 1, 10));
    assertEquals(period0.getEndDate(), adjusted.get(1));
    for (int i = 1; i < test.size(); i++) {
      SchedulePeriod period = test.getPeriod(i);
      assertEquals(period.getUnadjustedStartDate(), unadjusted.get(i));
      assertEquals(period.getUnadjustedEndDate(), unadjusted.get(i + 1));
      assertEquals(period.getStartDate(), adjusted.get(i));
      assertEquals(period.getEndDate(), adjusted.get(i + 1));
    }
    assertEquals(test.getFrequency(), freq);
    assertEquals(test.getRollConvention(), defn.calculatedRollConvention());
  }

  @Test(dataProvider = "generation")
  public void test_monthly_unadjusted(
      LocalDate start, LocalDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .businessDayAdjustment(BDA)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .build();
    ImmutableList<LocalDate> test = defn.createUnadjustedDates();
    assertEquals(test, unadjusted);
  }

  @Test(dataProvider = "generation")
  public void test_monthly_unadjusted_withOverride(
      LocalDate start, LocalDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .businessDayAdjustment(BDA)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .overrideStartDate(AdjustableDate.of(date(2011, 1, 9), BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .build();
    ImmutableList<LocalDate> test = defn.createUnadjustedDates();
    assertEquals(test.get(0), date(2011, 1, 9));
    assertEquals(test.subList(1, test.size()), unadjusted.subList(1, test.size()));
  }

  @Test(dataProvider = "generation")
  public void test_monthly_adjusted(
      LocalDate start, LocalDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .businessDayAdjustment(BDA)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .build();
    ImmutableList<LocalDate> test = defn.createAdjustedDates(REF_DATA);
    assertEquals(test, adjusted);
  }

  @Test(dataProvider = "generation")
  public void test_monthly_adjusted_withOverride(
      LocalDate start, LocalDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicSchedule defn = PeriodicSchedule.builder()
        .startDate(start)
        .endDate(end)
        .frequency(freq)
        .businessDayAdjustment(BDA)
        .stubConvention(stubConv)
        .rollConvention(rollConv)
        .firstRegularStartDate(firstReg)
        .lastRegularEndDate(lastReg)
        .overrideStartDate(AdjustableDate.of(date(2011, 1, 9), BusinessDayAdjustment.of(FOLLOWING, SAT_SUN)))
        .build();
    ImmutableList<LocalDate> test = defn.createAdjustedDates(REF_DATA);
    assertEquals(test.get(0), date(2011, 1, 10));
    assertEquals(test.subList(1, test.size()), adjusted.subList(1, test.size()));
  }

  //-------------------------------------------------------------------------
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
    assertEquals(test.calculatedStartDate(), AdjustableDate.of(date(2014, 10, 4), bda1));
    assertEquals(test.calculatedEndDate(), AdjustableDate.of(date(2015, 4, 4), bda2));
    assertEquals(test.createUnadjustedDates(), ImmutableList.of(date(2014, 10, 4), date(2015, 1, 4), date(2015, 4, 4)));
    assertEquals(test.createAdjustedDates(REF_DATA), ImmutableList.of(date(2014, 10, 3), date(2015, 1, 5), date(2015, 4, 3)));
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = ScheduleException.class)
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
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = ScheduleException.class)
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
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = ScheduleException.class)
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
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = ScheduleException.class)
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
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = ScheduleException.class)
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
    defn.createUnadjustedDates();
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = ScheduleException.class)
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
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = ScheduleException.class)
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
    defn.createUnadjustedDates();
  }

  //-------------------------------------------------------------------------
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
    assertEquals(test, ImmutableList.of(date(2015, 5, 29), date(2015, 5, 31)));
  }

  @Test(expectedExceptions = ScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
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
    defn.createAdjustedDates(REF_DATA);
  }

  @Test(expectedExceptions = ScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
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
    defn.createSchedule(REF_DATA);
  }

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
    assertEquals(test, ImmutableList.of(date(2015, 5, 27), date(2015, 5, 29), date(2015, 5, 31)));
  }

  @Test(expectedExceptions = ScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
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
    defn.createAdjustedDates(REF_DATA);
  }

  @Test(expectedExceptions = ScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
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
    defn.createSchedule(REF_DATA);
  }

  @Test(
      expectedExceptions = ScheduleException.class,
      expectedExceptionsMessageRegExp = "Schedule calculation resulted in invalid period")
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
    defn.createSchedule(REF_DATA);
  }

  @Test(expectedExceptions = ScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate unadjusted dates.*")
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
    defn.createUnadjustedDates();
  }

  //-------------------------------------------------------------------------
  @Test(dataProvider = "generation")
  public void coverage_equals(
      LocalDate start, LocalDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicSchedule a1 = of(start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule a2 = of(start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule b = of(LocalDate.MIN, end, freq, BDA, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule c = of(start, LocalDate.MAX, freq, BDA, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule d = of(
        start, end, freq == P1M ? P3M : P1M, BDA, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule e = of(
        start, end, freq, BusinessDayAdjustment.NONE, stubConv, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule f = of(
        start, end, freq, BDA, stubConv == STUB_NONE ? SHORT_FINAL : STUB_NONE, rollConv, firstReg, lastReg, null, null, null);
    PeriodicSchedule g = of(start, end, freq, BDA, stubConv, SFE, firstReg, lastReg, null, null, null);
    PeriodicSchedule h = of(start, end, freq, BDA, stubConv, rollConv, start.plusDays(1), null, null, null, null);
    PeriodicSchedule i = of(start, end, freq, BDA, stubConv, rollConv, null, end.minusDays(1), null, null, null);
    PeriodicSchedule j = of(start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg, BDA, null, null);
    PeriodicSchedule k = of(start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg, null, BDA, null);
    PeriodicSchedule m = of(
        start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg, null, null, AdjustableDate.of(start.minusDays(1)));
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.equals(d), false);
    assertEquals(a1.equals(e), false);
    assertEquals(a1.equals(f), false);
    assertEquals(a1.equals(g), false);
    assertEquals(a1.equals(h), false);
    assertEquals(a1.equals(i), false);
    assertEquals(a1.equals(j), false);
    assertEquals(a1.equals(k), false);
    assertEquals(a1.equals(m), false);
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

  public void coverage_builder() {
    PeriodicSchedule test = PeriodicSchedule.builder()
        .startDate(JUL_17)
        .endDate(SEP_17)
        .frequency(P2M)
        .businessDayAdjustment(BusinessDayAdjustment.NONE)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .stubConvention(STUB_NONE)
        .rollConvention(EOM)
        .firstRegularStartDate(JUL_17)
        .lastRegularEndDate(SEP_17)
        .overrideStartDate(AdjustableDate.of(JUL_11))
        .build();
    assertEquals(test.getStartDate(), JUL_17);
    assertEquals(test.getEndDate(), SEP_17);
    assertEquals(test.calculatedStartDate(), AdjustableDate.of(JUL_11, BusinessDayAdjustment.NONE));
    assertEquals(test.calculatedEndDate(), AdjustableDate.of(SEP_17, BusinessDayAdjustment.NONE));
  }

  //-------------------------------------------------------------------------
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
