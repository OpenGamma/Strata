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

import java.time.DayOfWeek;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.TestingReferenceDataId;

/**
 * Test {@link HolidaySafeReferenceData}.
 */
class HolidaySafeReferenceDataTest {

  private static final ImmutableList<DayOfWeek> WEEKEND_DAYS = ImmutableList.of(SATURDAY, SUNDAY);
  private static final HolidayCalendarId COMBINED_GBLO_EUTA_ID = HolidayCalendarId.of("GBLO+EUTA");
  private static final HolidayCalendarId LINKED_GBLO_EUTA_ID = HolidayCalendarId.of("GBLO~EUTA");
  private static final ImmutableHolidayCalendar GBLO_EUTA_CAL = ImmutableHolidayCalendar.combined(
      createCalendar(HolidayCalendarIds.GBLO), createCalendar(HolidayCalendarIds.EUTA));
  private static final TestingReferenceDataId ID1 = new TestingReferenceDataId("1");
  private static final TestingReferenceDataId ID2 = new TestingReferenceDataId("2");
  private static final Number VAL1 = 1;
  private static final Number VAL2 = 2;

  private static ImmutableHolidayCalendar createCalendar(HolidayCalendarId id) {
    return ImmutableHolidayCalendar.of(id, ImmutableList.of(), WEEKEND_DAYS);
  }

  @Test
  void test_singleCalendar_inReferenceData() {
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.minimal());

    assertThat(test.queryValueOrNull(HolidayCalendarIds.NO_HOLIDAYS)).isEqualTo(NoHolidaysCalendar.INSTANCE);
    assertThat(test.queryValueOrNull(HolidayCalendarIds.SAT_SUN)).isEqualTo(WeekendHolidayCalendar.SAT_SUN);
    assertThat(test.queryValueOrNull(HolidayCalendarIds.FRI_SAT)).isEqualTo(WeekendHolidayCalendar.FRI_SAT);
    assertThat(test.queryValueOrNull(HolidayCalendarIds.THU_FRI)).isEqualTo(WeekendHolidayCalendar.THU_FRI);
    assertThat(test.queryValueOrNull(HolidayCalendarIds.GBLO)).isEqualTo(createCalendar(HolidayCalendarIds.GBLO));
    assertThat(test.queryValueOrNull(HolidayCalendarId.of("TEST")))
        .isEqualTo(createCalendar(HolidayCalendarId.of("TEST")));
  }

  @Test
  void test_singleCalendar_missingFromReferenceData() {
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.empty());

    assertThat(test.queryValueOrNull(HolidayCalendarIds.NO_HOLIDAYS))
        .isEqualTo(createCalendar(HolidayCalendarIds.NO_HOLIDAYS));
    assertThat(test.queryValueOrNull(HolidayCalendarIds.SAT_SUN)).isEqualTo(createCalendar(HolidayCalendarIds.SAT_SUN));
    assertThat(test.queryValueOrNull(HolidayCalendarIds.FRI_SAT)).isEqualTo(createCalendar(HolidayCalendarIds.FRI_SAT));
    assertThat(test.queryValueOrNull(HolidayCalendarIds.THU_FRI)).isEqualTo(createCalendar(HolidayCalendarIds.THU_FRI));
    assertThat(test.queryValueOrNull(HolidayCalendarIds.GBLO)).isEqualTo(createCalendar(HolidayCalendarIds.GBLO));
    assertThat(test.queryValueOrNull(HolidayCalendarId.of("TEST")))
        .isEqualTo(createCalendar(HolidayCalendarId.of("TEST")));
  }

  @Test
  void test_combinedCalendar_inReferenceData() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(
        COMBINED_GBLO_EUTA_ID, GBLO_EUTA_CAL,
        LINKED_GBLO_EUTA_ID, GBLO_EUTA_CAL);
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.of(dataMap));

    assertThat(test.queryValueOrNull(COMBINED_GBLO_EUTA_ID)).isEqualTo(GBLO_EUTA_CAL);
    assertThat(test.queryValueOrNull(LINKED_GBLO_EUTA_ID)).isEqualTo(GBLO_EUTA_CAL);
  }

  @Test
  void test_combinedCalendar_missingFromReferenceData() {
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.empty());

    assertThat(test.queryValueOrNull(COMBINED_GBLO_EUTA_ID)).isNull();
    assertThat(test.queryValueOrNull(LINKED_GBLO_EUTA_ID)).isNull();
  }

  @Test
  void test_nonCalendarId() {
    HolidaySafeReferenceData test = new HolidaySafeReferenceData(ReferenceData.empty());

    assertThat(test.queryValueOrNull(ID1)).isNull();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test = ImmutableReferenceData.of(dataMap);
    coverImmutableBean(test);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ReferenceData test = ImmutableReferenceData.of(dataMap);
    assertSerialization(test);
  }

}
