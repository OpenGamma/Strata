/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

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
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.TestingNamedId;
import com.opengamma.strata.data.TestingObservableId;

/**
 * Test {@link ExtendedScenarioMarketData}.
 */
@Test
public class ExtendedScenarioMarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestingNamedId ID1 = new TestingNamedId("1");
  private static final TestingNamedId ID2 = new TestingNamedId("2");
  private static final TestingNamedId ID3 = new TestingNamedId("3");
  private static final TestingObservableId ID4 = new TestingObservableId("4");
  private static final MarketDataBox<String> VAL1 = MarketDataBox.ofSingleValue("1");
  private static final MarketDataBox<String> VAL2 = MarketDataBox.ofSingleValue("2");
  private static final MarketDataBox<String> VAL3 = MarketDataBox.ofSingleValue("3");
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final ImmutableScenarioMarketData BASE_DATA = baseData();

  //-------------------------------------------------------------------------
  public void of_addition() {
    ExtendedScenarioMarketData<String> test = ExtendedScenarioMarketData.of(ID3, VAL3, BASE_DATA);
    assertEquals(test.getId(), ID3);
    assertEquals(test.getValue(), VAL3);
    assertEquals(test.getValuationDate(), MarketDataBox.ofSingleValue(VAL_DATE));
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
    ExtendedScenarioMarketData<String> test = ExtendedScenarioMarketData.of(ID1, VAL3, BASE_DATA);
    assertEquals(test.getId(), ID1);
    assertEquals(test.getValue(), VAL3);
    assertEquals(test.getValuationDate(), MarketDataBox.ofSingleValue(VAL_DATE));
    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.containsValue(ID3), false);
    assertEquals(test.getValue(ID1), VAL3);
    assertEquals(test.getValue(ID2), VAL2);
    assertThrows(() -> test.getValue(ID3), MarketDataNotFoundException.class);
    assertEquals(test.getIds(), ImmutableSet.of(ID1, ID2));
    assertEquals(test.findValue(ID1), Optional.of(VAL3));
    assertEquals(test.findValue(ID2), Optional.of(VAL2));
    assertEquals(test.findValue(ID3), Optional.empty());
    assertEquals(test.getTimeSeries(ID4), TIME_SERIES);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExtendedScenarioMarketData<String> test = ExtendedScenarioMarketData.of(ID1, VAL1, BASE_DATA);
    coverImmutableBean(test);
    ExtendedScenarioMarketData<String> test2 = ExtendedScenarioMarketData.of(
        ID2,
        VAL2,
        ImmutableScenarioMarketData.of(3, VAL_DATE, ImmutableMap.of(), ImmutableMap.of()));
    coverBeanEquals(test, test2);
  }

  public void serialization() {
    ExtendedScenarioMarketData<String> test = ExtendedScenarioMarketData.of(ID1, VAL3, BASE_DATA);
    assertSerialization(test);
  }

  private static ImmutableScenarioMarketData baseData() {
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(ID4, TIME_SERIES);
    return ImmutableScenarioMarketData.builder(VAL_DATE)
        .addBox(ID1, VAL1)
        .addBox(ID2, VAL2)
        .addTimeSeriesMap(timeSeriesMap)
        .build();
  }

}
