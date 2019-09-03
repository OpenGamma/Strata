/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioArray;

/**
 * Test {@link ValuationZoneTimeDefinition}.
 */
public class ValuationZoneTimeDefinitionTest {

  private static final LocalTime LOCAL_TIME_1 = LocalTime.of(12, 20);
  private static final LocalTime LOCAL_TIME_2 = LocalTime.of(10, 10);
  private static final LocalTime LOCAL_TIME_3 = LocalTime.of(9, 35);
  private static final LocalTime LOCAL_TIME_4 = LocalTime.of(15, 12);
  private static final ZoneId ZONE_ID = ZoneId.of("America/Chicago");

  @Test
  public void test_of() {
    ValuationZoneTimeDefinition test = ValuationZoneTimeDefinition.of(
        LocalTime.MIDNIGHT, ZONE_ID, LOCAL_TIME_1, LOCAL_TIME_2, LOCAL_TIME_3);
    assertThat(test.getLocalTimes()).containsExactly(LOCAL_TIME_1, LOCAL_TIME_2, LOCAL_TIME_3);
    assertThat(test.getZoneId()).isEqualTo(ZONE_ID);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toZonedDateTime_scenario() {
    ValuationZoneTimeDefinition test = ValuationZoneTimeDefinition.of(
        LocalTime.MIDNIGHT, ZONE_ID, LOCAL_TIME_1, LOCAL_TIME_2, LOCAL_TIME_3);
    MarketDataBox<LocalDate> dates = MarketDataBox.ofScenarioValues(
        LocalDate.of(2016, 10, 21), LocalDate.of(2016, 10, 22), LocalDate.of(2016, 10, 23));
    MarketDataBox<ZonedDateTime> computed = test.toZonedDateTime(dates);
    MarketDataBox<ZonedDateTime> expected = MarketDataBox.ofScenarioValue(ScenarioArray.of(
        dates.getValue(0).atTime(LOCAL_TIME_1).atZone(ZONE_ID),
        dates.getValue(1).atTime(LOCAL_TIME_2).atZone(ZONE_ID),
        dates.getValue(2).atTime(LOCAL_TIME_3).atZone(ZONE_ID)));
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_toZonedDateTime_scenario_default() {
    ValuationZoneTimeDefinition test = ValuationZoneTimeDefinition.of(LOCAL_TIME_1, ZONE_ID);
    MarketDataBox<LocalDate> dates = MarketDataBox.ofScenarioValues(
        LocalDate.of(2016, 10, 21), LocalDate.of(2016, 10, 22), LocalDate.of(2016, 10, 23));
    MarketDataBox<ZonedDateTime> computed = test.toZonedDateTime(dates);
    MarketDataBox<ZonedDateTime> expected = MarketDataBox.ofScenarioValue(ScenarioArray.of(
        dates.getValue(0).atTime(LOCAL_TIME_1).atZone(ZONE_ID),
        dates.getValue(1).atTime(LOCAL_TIME_1).atZone(ZONE_ID),
        dates.getValue(2).atTime(LOCAL_TIME_1).atZone(ZONE_ID)));
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_toZonedDateTime_scenario_long() {
    ValuationZoneTimeDefinition test = ValuationZoneTimeDefinition.of(
        LOCAL_TIME_1, ZONE_ID, LOCAL_TIME_1, LOCAL_TIME_2);
    MarketDataBox<LocalDate> dates = MarketDataBox.ofScenarioValues(
        LocalDate.of(2016, 10, 21), LocalDate.of(2016, 10, 22), LocalDate.of(2016, 10, 23));
    MarketDataBox<ZonedDateTime> computed = test.toZonedDateTime(dates);
    MarketDataBox<ZonedDateTime> expected = MarketDataBox.ofScenarioValue(ScenarioArray.of(
        dates.getValue(0).atTime(LOCAL_TIME_1).atZone(ZONE_ID),
        dates.getValue(1).atTime(LOCAL_TIME_2).atZone(ZONE_ID),
        dates.getValue(2).atTime(LOCAL_TIME_1).atZone(ZONE_ID)));
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_toZonedDateTime_single() {
    ValuationZoneTimeDefinition test = ValuationZoneTimeDefinition.of(LOCAL_TIME_4, ZONE_ID);
    MarketDataBox<LocalDate> dates = MarketDataBox.ofSingleValue(LocalDate.of(2016, 10, 21));
    MarketDataBox<ZonedDateTime> computed = test.toZonedDateTime(dates);
    MarketDataBox<ZonedDateTime> expected = MarketDataBox.ofSingleValue(
        dates.getSingleValue().atTime(LOCAL_TIME_4).atZone(ZONE_ID));
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ValuationZoneTimeDefinition test1 = ValuationZoneTimeDefinition.of(LOCAL_TIME_1, ZONE_ID, LOCAL_TIME_2);
    coverImmutableBean(test1);
    ValuationZoneTimeDefinition test2 = ValuationZoneTimeDefinition.of(LOCAL_TIME_4, ZoneId.of("Europe/London"));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    ValuationZoneTimeDefinition test = ValuationZoneTimeDefinition.of(LOCAL_TIME_1, ZONE_ID);
    assertSerialization(test);
  }

}
