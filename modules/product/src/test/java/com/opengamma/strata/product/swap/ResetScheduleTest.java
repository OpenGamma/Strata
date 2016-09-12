/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_5;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.swap.IborRateResetMethod.UNWEIGHTED;
import static com.opengamma.strata.product.swap.IborRateResetMethod.WEIGHTED;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test.
 */
@Test
public class ResetScheduleTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_01_06 = date(2014, 1, 6);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
  private static final LocalDate DATE_04_07 = date(2014, 4, 7);

  //-------------------------------------------------------------------------
  public void test_builder_ensureDefaults() {
    ResetSchedule test = ResetSchedule.builder()
        .resetFrequency(P1M)
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
        .build();
    assertEquals(test.getResetFrequency(), P1M);
    assertEquals(test.getBusinessDayAdjustment(), BusinessDayAdjustment.of(FOLLOWING, GBLO));
    assertEquals(test.getResetMethod(), UNWEIGHTED);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResetSchedule test = ResetSchedule.builder()
        .resetFrequency(P1M)
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
        .build();
    SchedulePeriod accrualPeriod = SchedulePeriod.of(DATE_01_06, DATE_04_07, DATE_01_05, DATE_04_05);
    Schedule schedule = test.createSchedule(DAY_5, REF_DATA).apply(accrualPeriod);
    Schedule expected = Schedule.builder()
        .periods(
            SchedulePeriod.of(DATE_01_06, DATE_02_05, DATE_01_05, DATE_02_05),
            SchedulePeriod.of(DATE_02_05, DATE_03_05, DATE_02_05, DATE_03_05),
            SchedulePeriod.of(DATE_03_05, DATE_04_07, DATE_03_05, DATE_04_05))
        .frequency(P1M)
        .rollConvention(DAY_5)
        .build();
    assertEquals(schedule, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResetSchedule test = ResetSchedule.builder()
        .resetFrequency(P1M)
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
        .build();
    coverImmutableBean(test);
    ResetSchedule test2 = ResetSchedule.builder()
        .resetFrequency(P3M)
        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
        .resetMethod(WEIGHTED)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResetSchedule test = ResetSchedule.builder()
        .resetFrequency(P1M)
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
        .build();
    assertSerialization(test);
  }

}
