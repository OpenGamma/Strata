/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import static com.opengamma.basics.schedule.Frequency.P1M;
import static com.opengamma.basics.schedule.Frequency.P2M;
import static com.opengamma.basics.schedule.Frequency.P3M;
import static com.opengamma.basics.schedule.Frequency.TERM;
import static com.opengamma.basics.schedule.RollConventions.DAY_11;
import static com.opengamma.basics.schedule.RollConventions.DAY_17;
import static com.opengamma.basics.schedule.RollConventions.DAY_4;
import static com.opengamma.basics.schedule.RollConventions.EOM;
import static com.opengamma.basics.schedule.RollConventions.IMM;
import static com.opengamma.basics.schedule.RollConventions.SFE;
import static com.opengamma.basics.schedule.StubConvention.LONG_FINAL;
import static com.opengamma.basics.schedule.StubConvention.LONG_INITIAL;
import static com.opengamma.basics.schedule.StubConvention.SHORT_FINAL;
import static com.opengamma.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.date.AdjustableDate;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.BusinessDayConventions;
import com.opengamma.basics.date.HolidayCalendars;

/**
 * Test {@link PeriodicScheduleDefn}.
 */
@Test
public class PeriodicScheduleDefnTest {

  private static final StubConvention STUB_NONE = StubConvention.NONE;
  private static final StubConvention STUB_BOTH = StubConvention.BOTH;
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(
      BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendars.SAT_SUN);
  private static final LocalDate NOV_30_2013 = date(2013, NOVEMBER, 30);
  private static final LocalDate FEB_28 = date(2014, FEBRUARY, 28);
  private static final LocalDate MAY_30 = date(2014, MAY, 30);
  private static final LocalDate MAY_31 = date(2014, MAY, 31);
  private static final LocalDate AUG_30 = date(2014, AUGUST, 30);
  private static final LocalDate AUG_31 = date(2014, AUGUST, 31);
  private static final LocalDate NOV_30 = date(2014, NOVEMBER, 30);
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
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17);
  private static final AdjustableDate AD_JUN_04 = ad(JUN_04);
  private static final AdjustableDate AD_JUN_17 = ad(JUN_17);
  private static final AdjustableDate AD_JUL_17 = ad(JUL_17);
  private static final AdjustableDate AD_SEP_17 = ad(SEP_17);

  private static AdjustableDate ad(LocalDate date) {
    return AdjustableDate.of(date, BDA);
  }

  //-------------------------------------------------------------------------
  public void test_of_LocalDateEomFalse() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, false);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), SHORT_INITIAL);
    assertEquals(test.getRollConvention(), null);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), DAY_17);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_LocalDateEomTrue() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(JUN_04, SEP_17, P1M, BDA, SHORT_FINAL, true);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), SHORT_FINAL);
    assertEquals(test.getRollConvention(), EOM);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), DAY_4);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_LocalDateEom_null() {
    assertThrows(() -> PeriodicScheduleDefn.of(
        null, SEP_17, P1M, BDA, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, null, P1M, BDA, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, SEP_17, null, BDA, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, SEP_17, P1M, null, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, SEP_17, P1M, BDA, null, false), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_LocalDateRoll() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), SHORT_INITIAL);
    assertEquals(test.getRollConvention(), DAY_17);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), DAY_17);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_LocalDateRoll_null() {
    assertThrows(() -> PeriodicScheduleDefn.of(
        null, SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, null, P1M, BDA, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, SEP_17, null, BDA, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, SEP_17, P1M, null, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, SEP_17, P1M, BDA, null, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        JUN_04, SEP_17, P1M, BDA, SHORT_INITIAL, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_AdjustableDateEomFalse() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(AD_JUN_04, AD_SEP_17, P1M, BDA, SHORT_INITIAL, false);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), SHORT_INITIAL);
    assertEquals(test.getRollConvention(), null);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), DAY_17);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_AdjustableDateEomTrue() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(AD_JUN_04, AD_SEP_17, P1M, BDA, SHORT_FINAL, true);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), SHORT_FINAL);
    assertEquals(test.getRollConvention(), EOM);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), DAY_4);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_AdjustableDateEom_null() {
    assertThrows(() -> PeriodicScheduleDefn.of(
        null, AD_SEP_17, P1M, BDA, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, null, P1M, BDA, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, null, BDA, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, null, SHORT_INITIAL, false), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, false), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_AdjustableDateRoll() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(AD_JUN_04, AD_SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), SHORT_INITIAL);
    assertEquals(test.getRollConvention(), DAY_17);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), DAY_17);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_AdjustableDateRoll_null() {
    assertThrows(() -> PeriodicScheduleDefn.of(
        null, AD_SEP_17, P1M, BDA, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, null, P1M, BDA, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, null, BDA, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, null, SHORT_INITIAL, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, SHORT_INITIAL, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_All() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, SHORT_INITIAL, DAY_11, JUL_11, AUG_11);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), SHORT_INITIAL);
    assertEquals(test.getRollConvention(), DAY_11);
    assertEquals(test.getFirstRegularStartDate(), JUL_11);
    assertEquals(test.getLastRegularEndDate(), AUG_11);
    assertEquals(test.getEffectiveRollConvention(), DAY_11);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUL_11);
    assertEquals(test.getEffectiveLastRegularEndDate(), AUG_11);
  }

  public void test_of_All_allowNulls1() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, null, null, null);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), null);
    assertEquals(test.getRollConvention(), null);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), RollConventions.DAY_4);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_All_allowNulls2() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, null, null, AUG_04);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), null);
    assertEquals(test.getRollConvention(), null);
    assertEquals(test.getFirstRegularStartDate(), null);
    assertEquals(test.getLastRegularEndDate(), AUG_04);
    assertEquals(test.getEffectiveRollConvention(), RollConventions.DAY_4);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUN_04);
    assertEquals(test.getEffectiveLastRegularEndDate(), AUG_04);
  }

  public void test_of_All_allowNulls3() {
    PeriodicScheduleDefn test = PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, null, JUL_17, null);
    assertEquals(test.getStartDate(), AD_JUN_04);
    assertEquals(test.getEndDate(), AD_SEP_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BDA);
    assertEquals(test.getStubConvention(), null);
    assertEquals(test.getRollConvention(), null);
    assertEquals(test.getFirstRegularStartDate(), JUL_17);
    assertEquals(test.getLastRegularEndDate(), null);
    assertEquals(test.getEffectiveRollConvention(), RollConventions.DAY_17);
    assertEquals(test.getEffectiveFirstRegularStartDate(), JUL_17);
    assertEquals(test.getEffectiveLastRegularEndDate(), SEP_17);
  }

  public void test_of_All_null() {
    assertThrows(() -> PeriodicScheduleDefn.of(
        null, AD_SEP_17, P1M, BDA, SHORT_INITIAL, DAY_11, JUL_11, AUG_11), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, null, P1M, BDA, SHORT_INITIAL, DAY_11, JUL_11, AUG_11), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, null, BDA, SHORT_INITIAL, DAY_11, JUL_11, AUG_11), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, null, SHORT_INITIAL, DAY_11, JUL_11, AUG_11), IllegalArgumentException.class);
  }

  public void test_of_All_invalidDateOrder() {
    // start vs end
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_SEP_17, AD_SEP_17, P1M, BDA, null, null, null, null), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_SEP_17, AD_JUN_04, P1M, BDA, null, null, null, null), IllegalArgumentException.class);
    // first/last regular vs start/end
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, null, date(2014, JUNE, 3), null), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, null, null, date(2014, SEPTEMBER, 18)), IllegalArgumentException.class);
    // first regular vs last regular
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, null, date(2014, SEPTEMBER, 5), date(2014, SEPTEMBER, 5)), IllegalArgumentException.class);
    assertThrows(() -> PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, null, null, date(2014, SEPTEMBER, 5), date(2014, SEPTEMBER, 4)), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "generation")
  Object[][] data_generation() {
    return new Object[][] {
        // stub null
        {AD_JUN_17, AD_SEP_17, P1M, null, null, null, null,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        
        // stub NONE
        {AD_JUN_17, AD_SEP_17, P1M, STUB_NONE, null, null, null,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, AD_JUL_17, P1M, STUB_NONE, null, null, null,
          ImmutableList.of(JUN_17, JUL_17),
          ImmutableList.of(JUN_17, JUL_17)},
        
        // stub SHORT_INITIAL
        {AD_JUN_04, AD_SEP_17, P1M, SHORT_INITIAL, null, null, null,
          ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, AD_SEP_17, P1M, SHORT_INITIAL, null, null, null,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, ad(JUL_04), P1M, SHORT_INITIAL, null, null, null,
          ImmutableList.of(JUN_17, JUL_04),
          ImmutableList.of(JUN_17, JUL_04)},
        {ad(date(2011, 6, 28)), ad(date(2011, 6, 30)), P1M, SHORT_INITIAL, EOM, null, null,
          ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30)),
          ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30))},
        {ad(date(2014, 12, 12)), ad(date(2015, 8, 24)), P3M, SHORT_INITIAL, null, null, null,
          ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
          ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24))},
        {ad(date(2014, 12, 12)), ad(date(2015, 8, 24)), P3M, SHORT_INITIAL, RollConventions.NONE, null, null,
          ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 24), date(2015, 8, 24)),
          ImmutableList.of(date(2014, 12, 12), date(2015, 2, 24), date(2015, 5, 25), date(2015, 8, 24))},
        
        // stub LONG_INITIAL
        {AD_JUN_04, AD_SEP_17, P1M, LONG_INITIAL, null, null, null,
          ImmutableList.of(JUN_04, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_04, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, AD_SEP_17, P1M, LONG_INITIAL, null, null, null,
            ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
            ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, ad(JUL_04), P1M, LONG_INITIAL, null, null, null,
          ImmutableList.of(JUN_17, JUL_04),
          ImmutableList.of(JUN_17, JUL_04)},
        {AD_JUN_17, ad(AUG_04), P1M, LONG_INITIAL, null, null, null,
          ImmutableList.of(JUN_17, AUG_04),
          ImmutableList.of(JUN_17, AUG_04)},
        
        // stub SHORT_FINAL
        {AD_JUN_04, AD_SEP_17, P1M, SHORT_FINAL, null, null, null,
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17),
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_04, SEP_17)},
        {AD_JUN_17, AD_SEP_17, P1M, SHORT_FINAL, null, null, null,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, ad(JUL_04), P1M, SHORT_FINAL, null, null, null,
          ImmutableList.of(JUN_17, JUL_04),
          ImmutableList.of(JUN_17, JUL_04)},
        {ad(date(2011, 6, 28)), ad(date(2011, 6, 30)), P1M, SHORT_FINAL, EOM, null, null,
          ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30)),
          ImmutableList.of(date(2011, 6, 28), date(2011, 6, 30))},
        {ad(date(2014, 11, 29)), ad(date(2015, 9, 2)), P3M, SHORT_FINAL, null, null, null,
          ImmutableList.of(date(2014, 11, 29), date(2015, 2, 28), date(2015, 5, 29), date(2015, 8, 29), date(2015, 9, 2)),
          ImmutableList.of(date(2014, 11, 28), date(2015, 2, 27), date(2015, 5, 29), date(2015, 8, 31), date(2015, 9, 2))},
        {ad(date(2014, 11, 29)), ad(date(2015, 9, 2)), P3M, SHORT_FINAL, RollConventions.NONE, null, null,
          ImmutableList.of(date(2014, 11, 29), date(2015, 2, 28), date(2015, 5, 29), date(2015, 8, 29), date(2015, 9, 2)),
          ImmutableList.of(date(2014, 11, 28), date(2015, 2, 27), date(2015, 5, 29), date(2015, 8, 31), date(2015, 9, 2))},
        
        // stub LONG_FINAL
        {AD_JUN_04, AD_SEP_17, P1M, LONG_FINAL, null, null, null,
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17),
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17)},
        {AD_JUN_17, AD_SEP_17, P1M, LONG_FINAL, null, null, null,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, ad(JUL_04), P1M, LONG_FINAL, null, null, null,
          ImmutableList.of(JUN_17, JUL_04),
          ImmutableList.of(JUN_17, JUL_04)},
        {AD_JUN_17, ad(AUG_04), P1M, LONG_FINAL, null, null, null,
          ImmutableList.of(JUN_17, AUG_04),
          ImmutableList.of(JUN_17, AUG_04)},
        
        // explicit initial stub
        {AD_JUN_04, AD_SEP_17, P1M, null, null, JUN_17, null,
          ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_04, AD_SEP_17, P1M, SHORT_INITIAL, null, JUN_17, null,
          ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_04, JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_17, AD_SEP_17, P1M, null, null, JUN_17, null,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        
        // explicit final stub
        {AD_JUN_04, AD_SEP_17, P1M, null, null, null, AUG_04,
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17),
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17)},
        {AD_JUN_04, AD_SEP_17, P1M, SHORT_FINAL, null, null, AUG_04,
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17),
          ImmutableList.of(JUN_04, JUL_04, AUG_04, SEP_17)},
        {AD_JUN_17, AD_SEP_17, P1M, null, null, null, AUG_17,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        
        // explicit double stub
        {AD_JUN_04, AD_SEP_17, P1M, null, null, JUL_11, AUG_11,
          ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17),
          ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17)},
        {AD_JUN_17, AD_SEP_17, P1M, null, null, JUN_17, SEP_17,
          ImmutableList.of(JUN_17, JUL_17, AUG_17, SEP_17),
          ImmutableList.of(JUN_17, JUL_17, AUG_18, SEP_17)},
        {AD_JUN_04, AD_SEP_17, P1M, STUB_BOTH, null, JUL_11, AUG_11,
          ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17),
          ImmutableList.of(JUN_04, JUL_11, AUG_11, SEP_17)},
        
        // near end of month
        // EOM flag false, thus roll on 30th
        {ad(NOV_30_2013), ad(NOV_30), P3M, STUB_NONE, null, null, null,
          ImmutableList.of(NOV_30_2013, FEB_28, MAY_30, AUG_30, NOV_30),
          ImmutableList.of(date(2013, NOVEMBER, 29), FEB_28, MAY_30, date(2014, AUGUST, 29), date(2014, NOVEMBER, 28))},
        // EOM flag true and is EOM, thus roll at EOM
        {ad(NOV_30_2013), ad(NOV_30), P3M, STUB_NONE, EOM, null, null,
          ImmutableList.of(NOV_30_2013, FEB_28, MAY_31, AUG_31, NOV_30),
          ImmutableList.of(date(2013, NOVEMBER, 29), FEB_28, MAY_30, date(2014, AUGUST, 29), date(2014, NOVEMBER, 28))},
        // EOM flag true, but not EOM, thus roll on 30th
        {ad(MAY_30), ad(NOV_30), P3M, STUB_NONE, EOM, null, null,
          ImmutableList.of(MAY_30, AUG_30, NOV_30),
          ImmutableList.of(MAY_30, date(2014, AUGUST, 29), date(2014, NOVEMBER, 28))},
        // EOM flag true and is EOM, double stub, thus roll at EOM
        {ad(date(2014, 1, 3)), ad(SEP_17), P3M, STUB_BOTH, EOM, FEB_28, AUG_31,
          ImmutableList.of(date(2014, 1, 3), FEB_28, MAY_31, AUG_31, SEP_17),
          ImmutableList.of(date(2014, 1, 3), FEB_28, MAY_30, date(2014, AUGUST, 29), SEP_17)},
        
        // TERM period
        {AD_JUN_04, AD_SEP_17, TERM, STUB_NONE, null, null, null,
          ImmutableList.of(JUN_04, SEP_17),
          ImmutableList.of(JUN_04, SEP_17)},
        
        // IMM
        {ad(date(2014, 9, 17)), ad(date(2014, 10, 15)), P1M, STUB_NONE, IMM, null, null,
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15)),
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15))},
        {ad(date(2014, 9, 17)), ad(date(2014, 10, 15)), TERM, STUB_NONE, IMM, null, null,
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15)),
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15))},
        // IMM with stupid short period still works
        {ad(date(2014, 9, 17)), ad(date(2014, 10, 15)), Frequency.ofDays(2), STUB_NONE, IMM, null, null,
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15)),
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 15))},
        {ad(date(2014, 9, 17)), ad(date(2014, 10, 1)), Frequency.ofDays(2), STUB_NONE, IMM, null, null,
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 1)),
          ImmutableList.of(date(2014, 9, 17), date(2014, 10, 1))},
    };
  }

  @Test(dataProvider = "generation")
  public void test_monthly_schedule(
      AdjustableDate start, AdjustableDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg);
    PeriodicSchedule test = defn.createSchedule();
    assertEquals(test.size(), unadjusted.size() - 1);
    for (int i = 0; i < test.size(); i++) {
      SchedulePeriod period = test.getPeriod(i);
      if (test.getPeriods().size() == 1) {
        assertEquals(period.getType(), SchedulePeriodType.TERM);
      } else if (i == 0) {
        assertEquals(period.getType(), SchedulePeriodType.INITIAL);
      } else if (i == test.getPeriods().size() - 1) {
        assertEquals(period.getType(), SchedulePeriodType.FINAL);
      } else {
        assertEquals(period.getType(), SchedulePeriodType.NORMAL);
      }
      assertEquals(period.getUnadjustedStartDate(), unadjusted.get(i));
      assertEquals(period.getUnadjustedEndDate(), unadjusted.get(i + 1));
      assertEquals(period.getStartDate(), adjusted.get(i));
      assertEquals(period.getEndDate(), adjusted.get(i + 1));
      assertEquals(period.getFrequency(), freq);
      assertEquals(period.getRollConvention(), defn.getEffectiveRollConvention());
    }
  }
  
  @Test(dataProvider = "generation")
  public void test_monthly_unadjusted(
      AdjustableDate start, AdjustableDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg);
    ImmutableList<LocalDate> test = defn.createUnadjustedDates();
    assertEquals(test, unadjusted);
  }

  @Test(dataProvider = "generation")
  public void test_monthly_adjusted(
      AdjustableDate start, AdjustableDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg);
    ImmutableList<LocalDate> test = defn.createAdjustedDates();
    assertEquals(test, adjusted);
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = PeriodicScheduleException.class)
  public void test_none_badStub() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, P1M, BDA, STUB_NONE, DAY_4, null, null);
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = PeriodicScheduleException.class)
  public void test_both_badStub() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AD_JUN_17, AD_SEP_17, P1M, BDA, STUB_BOTH, null, JUN_17, SEP_17);
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = PeriodicScheduleException.class)
  public void test_backwards_badStub() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AD_JUL_17, AD_SEP_17, P1M, BDA, SHORT_INITIAL, DAY_11, null, null);
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = PeriodicScheduleException.class)
  public void test_forwards_badStub() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AD_JUL_17, AD_SEP_17, P1M, BDA, SHORT_FINAL, DAY_11, null, null);
    defn.createUnadjustedDates();
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = PeriodicScheduleException.class)
  public void test_termFrequency_badInitialStub() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, TERM, BDA, STUB_NONE, DAY_4, JUN_17, null);
    defn.createUnadjustedDates();
  }

  @Test(expectedExceptions = PeriodicScheduleException.class)
  public void test_termFrequency_badFinalStub() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AD_JUN_04, AD_SEP_17, TERM, BDA, STUB_NONE, DAY_4, null, SEP_04);
    defn.createUnadjustedDates();
  }

  //-------------------------------------------------------------------------
  public void test_emptyWhenAdjusted_term_createUnadjustedDates() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        ad(date(2015, 5, 29)), ad(date(2015, 5, 31)), TERM, BDA, null, null, null, null);
    ImmutableList<LocalDate> test = defn.createUnadjustedDates();
    assertEquals(test, ImmutableList.of(date(2015, 5, 29), date(2015, 5, 31)));
  }

  @Test(expectedExceptions = PeriodicScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
  public void test_emptyWhenAdjusted_term_createAdjustedDates() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        ad(date(2015, 5, 29)), ad(date(2015, 5, 31)), TERM, BDA, null, null, null, null);
    defn.createAdjustedDates();
  }

  @Test(expectedExceptions = PeriodicScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
  public void test_emptyWhenAdjusted_term_createSchedule() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        ad(date(2015, 5, 29)), ad(date(2015, 5, 31)), TERM, BDA, null, null, null, null);
    defn.createSchedule();
  }

  public void test_emptyWhenAdjusted_twoPeriods_createUnadjustedDates() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        ad(date(2015, 5, 27)), ad(date(2015, 5, 31)), Frequency.ofDays(2), BDA, STUB_NONE, null, null, null);
    ImmutableList<LocalDate> test = defn.createUnadjustedDates();
    assertEquals(test, ImmutableList.of(date(2015, 5, 27), date(2015, 5, 29), date(2015, 5, 31)));
  }

  @Test(expectedExceptions = PeriodicScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
  public void test_emptyWhenAdjusted_twoPeriods_createAdjustedDates() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        ad(date(2015, 5, 27)), ad(date(2015, 5, 31)), Frequency.ofDays(2), BDA, STUB_NONE, null, null, null);
    defn.createAdjustedDates();
  }

  @Test(expectedExceptions = PeriodicScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate adjusted dates.*")
  public void test_emptyWhenAdjusted_twoPeriods_createSchedule() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        ad(date(2015, 5, 27)), ad(date(2015, 5, 31)), Frequency.ofDays(2), BDA, STUB_NONE, null, null, null);
    defn.createSchedule();
  }

  @Test(expectedExceptions = PeriodicScheduleException.class, expectedExceptionsMessageRegExp = ".*duplicate unadjusted dates.*")
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
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        ad(date(2015, 5, 27)), ad(date(2015, 5, 31)), Frequency.ofDays(2), BDA, STUB_NONE, roll, null, null);
    defn.createUnadjustedDates();
  }

  //-------------------------------------------------------------------------
  @Test(dataProvider = "generation")
  public void coverage_equals(
      AdjustableDate start, AdjustableDate end, Frequency freq, StubConvention stubConv, RollConvention rollConv,
      LocalDate firstReg, LocalDate lastReg, List<LocalDate> unadjusted, List<LocalDate> adjusted) {
    PeriodicScheduleDefn a1 = PeriodicScheduleDefn.of(
        start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg);
    PeriodicScheduleDefn a2 = PeriodicScheduleDefn.of(
        start, end, freq, BDA, stubConv, rollConv, firstReg, lastReg);
    PeriodicScheduleDefn b = PeriodicScheduleDefn.of(
        ad(LocalDate.MIN), end, freq, BDA, stubConv, rollConv, firstReg, lastReg);
    PeriodicScheduleDefn c = PeriodicScheduleDefn.of(
        start, ad(LocalDate.MAX), freq, BDA, stubConv, rollConv, firstReg, lastReg);
    PeriodicScheduleDefn d = PeriodicScheduleDefn.of(
        start, end, freq == P1M ? P3M : P1M, BDA, stubConv, rollConv, firstReg, lastReg);
    PeriodicScheduleDefn e = PeriodicScheduleDefn.of(
        start, end, freq, BusinessDayAdjustment.NONE, stubConv, rollConv, firstReg, lastReg);
    PeriodicScheduleDefn f = PeriodicScheduleDefn.of(
        start, end, freq, BDA, stubConv == STUB_NONE ? SHORT_FINAL : STUB_NONE, rollConv, firstReg, lastReg);
    PeriodicScheduleDefn g = PeriodicScheduleDefn.of(
        start, end, freq, BDA, stubConv, SFE, firstReg, lastReg);
    PeriodicScheduleDefn h = PeriodicScheduleDefn.of(
        start, end, freq, BDA, stubConv, rollConv, start.getUnadjusted().plusDays(1), lastReg);
    PeriodicScheduleDefn i = PeriodicScheduleDefn.of(
        start, end, freq, BDA, stubConv, rollConv, firstReg, end.getUnadjusted().minusDays(1));
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
  }

  public void coverage_builder() {
    PeriodicScheduleDefn.Builder builder = PeriodicScheduleDefn.builder();
    builder
      .startDate(AD_JUL_17)
      .endDate(AD_SEP_17)
      .frequency(P2M)
      .businessDayAdjustment(BusinessDayAdjustment.NONE)
      .stubConvention(STUB_NONE)
      .rollConvention(EOM)
      .firstRegularStartDate(JUL_17)
      .lastRegularEndDate(SEP_17)
      .build();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN);
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AdjustableDate.of(date(2014, JUNE, 4), bda),
        AdjustableDate.of(date(2014, SEPTEMBER, 17), bda),
        P1M, 
        bda,
        SHORT_INITIAL,
        false);
    coverImmutableBean(defn);
  }

  public void test_serialization() {
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN);
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        AdjustableDate.of(date(2014, JUNE, 4), bda),
        AdjustableDate.of(date(2014, SEPTEMBER, 17), bda),
        P1M, 
        bda,
        SHORT_INITIAL,
        false);
    assertSerialization(defn);
  }

}
