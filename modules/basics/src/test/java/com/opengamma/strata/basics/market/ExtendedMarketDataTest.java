/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link ExtendedMarketData}.
 */
@Test
public class ExtendedMarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestObservableKey KEY1 = TestObservableKey.of("1");
  private static final TestObservableKey KEY2 = TestObservableKey.of("2");
  private static final TestObservableKey KEY3 = TestObservableKey.of("3");
  private static final TestObservableKey KEY4 = TestObservableKey.of("4");
  private static final Double VAL1 = 123d;
  private static final Double VAL2 = 234d;
  private static final Double VAL3 = 999d;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final ImmutableMarketData BASE_DATA = baseData();

  //-------------------------------------------------------------------------
  public void of_addition() {
    ExtendedMarketData<Double> test = ExtendedMarketData.of(KEY3, VAL3, BASE_DATA);
    assertEquals(test.getKey(), KEY3);
    assertEquals(test.getValue(), VAL3);
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(KEY1), true);
    assertEquals(test.containsValue(KEY2), true);
    assertEquals(test.containsValue(KEY3), true);
    assertEquals(test.containsValue(KEY4), false);
    assertEquals(test.getValue(KEY1), VAL1);
    assertEquals(test.getValue(KEY2), VAL2);
    assertEquals(test.getValue(KEY3), VAL3);
    assertThrows(() -> test.getValue(KEY4), MarketDataNotFoundException.class);
    assertEquals(test.findValue(KEY1), Optional.of(VAL1));
    assertEquals(test.findValue(KEY2), Optional.of(VAL2));
    assertEquals(test.findValue(KEY3), Optional.of(VAL3));
    assertEquals(test.findValue(KEY4), Optional.empty());
    assertEquals(test.getTimeSeries(KEY2), TIME_SERIES);
  }

  public void of_override() {
    ExtendedMarketData<Double> test = ExtendedMarketData.of(KEY1, VAL3, BASE_DATA);
    assertEquals(test.getKey(), KEY1);
    assertEquals(test.getValue(), VAL3);
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(KEY1), true);
    assertEquals(test.containsValue(KEY2), true);
    assertEquals(test.containsValue(KEY3), false);
    assertEquals(test.getValue(KEY1), VAL3);
    assertEquals(test.getValue(KEY2), VAL2);
    assertThrows(() -> test.getValue(KEY3), MarketDataNotFoundException.class);
    assertEquals(test.findValue(KEY1), Optional.of(VAL3));
    assertEquals(test.findValue(KEY2), Optional.of(VAL2));
    assertEquals(test.findValue(KEY3), Optional.empty());
    assertEquals(test.getTimeSeries(KEY2), TIME_SERIES);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExtendedMarketData<Double> test = ExtendedMarketData.of(KEY1, VAL1, BASE_DATA);
    coverImmutableBean(test);
    ExtendedMarketData<Double> test2 = ExtendedMarketData.of(KEY2, VAL2, ImmutableMarketData.of(VAL_DATE, ImmutableMap.of()));
    coverBeanEquals(test, test2);
  }

  public void serialization() {
    ExtendedMarketData<Double> test = ExtendedMarketData.of(KEY1, VAL3, BASE_DATA);
    assertSerialization(test);
  }

  private static ImmutableMarketData baseData() {
    Map<MarketDataKey<?>, Object> dataMap = ImmutableMap.of(KEY1, VAL1, KEY2, VAL2);
    Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(KEY2, TIME_SERIES);
    return ImmutableMarketData.builder(VAL_DATE).values(dataMap).timeSeries(timeSeriesMap).build();
  }

}
