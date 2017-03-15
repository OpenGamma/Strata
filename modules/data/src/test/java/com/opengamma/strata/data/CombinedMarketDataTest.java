/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link CombinedMarketData}.
 */
@Test
public class CombinedMarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestingNamedId ID1 = new TestingNamedId("1");
  private static final TestingNamedId ID2 = new TestingNamedId("2");
  private static final TestingNamedId ID3 = new TestingNamedId("3");
  private static final TestingNamedId ID4 = new TestingNamedId("4");
  private static final TestingObservableId ID5 = new TestingObservableId("5");
  private static final TestingObservableId ID6 = new TestingObservableId("6");
  private static final String VAL1 = "1";
  private static final String VAL2 = "2";
  private static final String VAL3 = "3";
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final ImmutableMarketData BASE_DATA1 = baseData1();
  private static final ImmutableMarketData BASE_DATA2 = baseData2();

  //-------------------------------------------------------------------------
  public void test_combination() {
    CombinedMarketData test = new CombinedMarketData(BASE_DATA1, BASE_DATA2);
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.containsValue(ID3), true);
    assertEquals(test.containsValue(ID5), false);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
    assertEquals(test.getValue(ID3), VAL3);
    assertThrows(() -> test.getValue(ID5), MarketDataNotFoundException.class);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));
    assertEquals(test.findValue(ID2), Optional.of(VAL2));
    assertEquals(test.findValue(ID3), Optional.of(VAL3));
    assertEquals(test.findValue(ID5), Optional.empty());
    assertEquals(test.getIds(), ImmutableSet.of(ID1, ID2, ID3));
    assertEquals(test.findIds(ID1.getMarketDataName()), ImmutableSet.of(ID1));
    assertEquals(test.findIds(ID3.getMarketDataName()), ImmutableSet.of(ID3));
    assertEquals(test.findIds(ID4.getMarketDataName()), ImmutableSet.of());
    assertEquals(test.getTimeSeries(ID5), TIME_SERIES);
    assertEquals(test.getTimeSeries(ID6), TIME_SERIES);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CombinedMarketData test = new CombinedMarketData(BASE_DATA1, BASE_DATA2);
    coverImmutableBean(test);
    CombinedMarketData test2 = new CombinedMarketData(BASE_DATA2, BASE_DATA1);
    coverBeanEquals(test, test2);
  }

  public void serialization() {
    CombinedMarketData test = new CombinedMarketData(BASE_DATA1, BASE_DATA2);
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  private static ImmutableMarketData baseData1() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(ID5, TIME_SERIES);
    return ImmutableMarketData.builder(VAL_DATE).values(dataMap).timeSeries(timeSeriesMap).build();
  }

  private static ImmutableMarketData baseData2() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL3, ID3, VAL3);
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(ID6, TIME_SERIES);
    return ImmutableMarketData.builder(VAL_DATE).values(dataMap).timeSeries(timeSeriesMap).build();
  }

}
