/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.DayOfWeek;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.TestingReferenceDataId;

/**
 * Test {@link HolidaySafeReferenceData}.
 */
class HolidaySafeReferenceDataTest {

  private static final ImmutableList<DayOfWeek> WEEKEND_DAYS = ImmutableList.of(SATURDAY, SUNDAY);
  private static final HolidayCalendarId NO_HOL_ID = HolidayCalendarIds.NO_HOLIDAYS;
  private static final HolidayCalendar NO_HOL_CAL = HolidayCalendars.NO_HOLIDAYS;
  private static final HolidayCalendarId SAT_SUN_ID = HolidayCalendarIds.SAT_SUN;
  private static final HolidayCalendar SAT_SUN_CAL = HolidayCalendars.SAT_SUN;
  private static final HolidayCalendarId GBLO_ID = HolidayCalendarIds.GBLO;
  private static final HolidayCalendar GBLO_CAL = HolidayCalendars.of(GBLO_ID.getName());
  private static final HolidayCalendarId EUTA_ID = HolidayCalendarIds.EUTA;
  private static final HolidayCalendar EUTA_CAL = HolidayCalendars.of(EUTA_ID.getName());
  private static final HolidayCalendar TEST_EUTA_CAL = createCalendar(EUTA_ID);
  private static final HolidayCalendarId TEST_ID = HolidayCalendarId.of("TEST");
  private static final HolidayCalendar TEST_CAL = createCalendar(TEST_ID);
  private static final HolidayCalendarId COMBINED_GBLO_EUTA_ID = HolidayCalendarId.of("GBLO+EUTA");
  private static final HolidayCalendarId LINKED_GBLO_EUTA_ID = HolidayCalendarId.of("GBLO~EUTA");
  private static final TestingReferenceDataId NON_CAL_ID = new TestingReferenceDataId("1");
  private static final Number NON_CAL_VAL = 1;

  private static ImmutableHolidayCalendar createCalendar(HolidayCalendarId id) {
    return ImmutableHolidayCalendar.of(id, ImmutableList.of(), WEEKEND_DAYS);
  }

  @Test
  void test_singleCalendar_inReferenceData() {
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.minimal());

    assertThat(test.queryValueOrNull(NO_HOL_ID)).isEqualTo(NoHolidaysCalendar.INSTANCE);
    assertThat(test.queryValueOrNull(SAT_SUN_ID)).isEqualTo(WeekendHolidayCalendar.SAT_SUN);
    assertThat(test.queryValueOrNull(GBLO_ID)).isEqualTo(createCalendar(GBLO_ID));
    assertThat(test.queryValueOrNull(TEST_ID)).isEqualTo(TEST_CAL);
  }

  @Test
  void test_singleCalendar_missingFromReferenceData() {
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.empty());

    assertThat(test.queryValueOrNull(NO_HOL_ID)).isEqualTo(createCalendar(NO_HOL_ID));
    assertThat(test.queryValueOrNull(SAT_SUN_ID)).isEqualTo(createCalendar(SAT_SUN_ID));
    assertThat(test.queryValueOrNull(GBLO_ID)).isEqualTo(createCalendar(GBLO_ID));
    assertThat(test.queryValueOrNull(TEST_ID)).isEqualTo(TEST_CAL);
  }

  @Test
  void test_singleCalendar_getValue() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(
        NO_HOL_ID, NO_HOL_CAL,
        SAT_SUN_ID, SAT_SUN_CAL,
        GBLO_ID, GBLO_CAL);
    ReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));

    assertThat(test.getValue(NO_HOL_ID)).isEqualTo(NO_HOL_CAL);
    assertThat(test.getValue(SAT_SUN_ID)).isEqualTo(SAT_SUN_CAL);
    assertThat(test.getValue(GBLO_ID)).isEqualTo(GBLO_CAL);
    assertThat(test.getValue(TEST_ID)).isEqualTo(TEST_CAL);
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> test.getValue(NON_CAL_ID));
  }

  @Test
  void test_singleCalendar_findValue() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(
        NO_HOL_ID, NO_HOL_CAL,
        SAT_SUN_ID, SAT_SUN_CAL,
        GBLO_ID, GBLO_CAL);
    ReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));

    assertThat(test.findValue(NO_HOL_ID)).contains(NO_HOL_CAL);
    assertThat(test.findValue(SAT_SUN_ID)).contains(SAT_SUN_CAL);
    assertThat(test.findValue(GBLO_ID)).contains(GBLO_CAL);
    assertThat(test.findValue(TEST_ID)).contains(TEST_CAL);
    assertThat(test.findValue(NON_CAL_ID)).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  void test_combinedCalendar_getValue_referenceDataContainsBoth() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(
        EUTA_ID, EUTA_CAL,
        GBLO_ID, GBLO_CAL);
    ReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));

    assertThat(test.getValue(COMBINED_GBLO_EUTA_ID)).isEqualTo(new CombinedHolidayCalendar(EUTA_CAL, GBLO_CAL));
    assertThat(test.getValue(LINKED_GBLO_EUTA_ID)).isEqualTo(new LinkedHolidayCalendar(EUTA_CAL, GBLO_CAL));
  }

  @Test
  void test_combinedCalendar_getValue_referenceDataOnlyContainsOne() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(
        GBLO_ID, GBLO_CAL);
    ReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));

    assertThat(test.getValue(COMBINED_GBLO_EUTA_ID)).isEqualTo(new CombinedHolidayCalendar(TEST_EUTA_CAL, GBLO_CAL));
    assertThat(test.getValue(LINKED_GBLO_EUTA_ID)).isEqualTo(new LinkedHolidayCalendar(TEST_EUTA_CAL, GBLO_CAL));
  }

  @Test
  void test_combinedCalendar_findValue_referenceDataContainsBoth() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(
        EUTA_ID, EUTA_CAL,
        GBLO_ID, GBLO_CAL);
    ReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));

    assertThat(test.findValue(COMBINED_GBLO_EUTA_ID)).contains(new CombinedHolidayCalendar(EUTA_CAL, GBLO_CAL));
    assertThat(test.findValue(LINKED_GBLO_EUTA_ID)).contains(new LinkedHolidayCalendar(EUTA_CAL, GBLO_CAL));
  }

  @Test
  void test_combinedCalendar_findValue_referenceDataOnlyContainsOne() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(
        GBLO_ID, GBLO_CAL);
    ReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));

    assertThat(test.findValue(COMBINED_GBLO_EUTA_ID)).contains(new CombinedHolidayCalendar(TEST_EUTA_CAL, GBLO_CAL));
    assertThat(test.findValue(LINKED_GBLO_EUTA_ID)).contains(new LinkedHolidayCalendar(TEST_EUTA_CAL, GBLO_CAL));
  }

  //-------------------------------------------------------------------------
  @Test
  void test_nonCalendarId() {
    ReferenceData test = new HolidaySafeReferenceData(ReferenceData.empty());

    assertThat(test.queryValueOrNull(NON_CAL_ID)).isNull();
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> test.getValue(NON_CAL_ID));
    assertThat(test.findValue(NON_CAL_ID)).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(NON_CAL_ID, NON_CAL_VAL);
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));
    coverImmutableBean(test);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(TEST_ID, TEST_CAL);
    HolidaySafeReferenceData test2 = new HolidaySafeReferenceData(ReferenceData.of(dataMap2));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(TEST_ID, TEST_CAL);
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));
    assertSerialization(test);
  }

}
