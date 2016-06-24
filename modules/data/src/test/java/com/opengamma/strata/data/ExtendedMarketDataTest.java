/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

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
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link ExtendedMarketData}.
 */
@Test
public class ExtendedMarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestingNamedId ID1 = new TestingNamedId("1");
  private static final TestingNamedId ID2 = new TestingNamedId("2");
  private static final TestingNamedId ID3 = new TestingNamedId("3");
  private static final TestingObservableId ID4 = new TestingObservableId("4");
  private static final String VAL1 = "1";
  private static final String VAL2 = "2";
  private static final String VAL3 = "3";
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final ImmutableMarketData BASE_DATA = baseData();

  //-------------------------------------------------------------------------
  public void of_addition() {
    ExtendedMarketData<String> test = ExtendedMarketData.of(ID3, VAL3, BASE_DATA);
    assertEquals(test.getId(), ID3);
    assertEquals(test.getValue(), VAL3);
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.containsValue(ID3), true);
    assertEquals(test.containsValue(ID4), false);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
    assertEquals(test.getValue(ID3), VAL3);
    assertThrows(() -> test.getValue(ID4), MarketDataNotFoundException.class);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));
    assertEquals(test.findValue(ID2), Optional.of(VAL2));
    assertEquals(test.findValue(ID3), Optional.of(VAL3));
    assertEquals(test.findValue(ID4), Optional.empty());
    assertEquals(test.getIds(), ImmutableSet.of(ID1, ID2, ID3));
    assertEquals(test.findIds(ID1.getMarketDataName()), ImmutableSet.of(ID1));
    assertEquals(test.findIds(ID3.getMarketDataName()), ImmutableSet.of(ID3));
    assertEquals(test.getTimeSeries(ID4), TIME_SERIES);
  }

  public void of_override() {
    ExtendedMarketData<String> test = ExtendedMarketData.of(ID1, VAL3, BASE_DATA);
    assertEquals(test.getId(), ID1);
    assertEquals(test.getValue(), VAL3);
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.containsValue(ID3), false);
    assertEquals(test.getValue(ID1), VAL3);
    assertEquals(test.getValue(ID2), VAL2);
    assertThrows(() -> test.getValue(ID3), MarketDataNotFoundException.class);
    assertEquals(test.findValue(ID1), Optional.of(VAL3));
    assertEquals(test.findValue(ID2), Optional.of(VAL2));
    assertEquals(test.findValue(ID3), Optional.empty());
    assertEquals(test.getIds(), ImmutableSet.of(ID1, ID2));
    assertEquals(test.getTimeSeries(ID4), TIME_SERIES);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExtendedMarketData<String> test = ExtendedMarketData.of(ID1, VAL1, BASE_DATA);
    coverImmutableBean(test);
    ExtendedMarketData<String> test2 = ExtendedMarketData.of(ID2, VAL2, ImmutableMarketData.of(VAL_DATE, ImmutableMap.of()));
    coverBeanEquals(test, test2);
  }

  public void serialization() {
    ExtendedMarketData<String> test = ExtendedMarketData.of(ID1, VAL3, BASE_DATA);
    assertSerialization(test);
  }

  private static ImmutableMarketData baseData() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(ID4, TIME_SERIES);
    return ImmutableMarketData.builder(VAL_DATE).values(dataMap).timeSeries(timeSeriesMap).build();
  }

}
