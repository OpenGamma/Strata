/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static org.testng.Assert.assertSame;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.TestingNamedId;
import com.opengamma.strata.data.TestingObservableId;

/**
 * Test {@link RepeatedScenarioMarketData}.
 */
@Test
public class RepeatedScenarioMarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestingNamedId ID1 = new TestingNamedId("1");
  private static final TestingNamedId ID2 = new TestingNamedId("2");
  private static final TestingNamedId ID3 = new TestingNamedId("3");
  private static final TestingObservableId ID4 = new TestingObservableId("4");
  private static final String VAL1 = "1";
  private static final String VAL2 = "2";
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final ImmutableMarketData BASE_DATA = baseData();

  //-------------------------------------------------------------------------
  public void test_of() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    assertEquals(test.getScenarioCount(), 2);
    assertEquals(test.getUnderlying(), BASE_DATA);
    assertEquals(test.getValuationDate(), MarketDataBox.ofSingleValue(VAL_DATE));
    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.containsValue(ID3), false);
    assertEquals(test.getValue(ID1), MarketDataBox.ofSingleValue(VAL1));
    assertEquals(test.getValue(ID2), MarketDataBox.ofSingleValue(VAL2));
    assertThrows(() -> test.getValue(ID3), MarketDataNotFoundException.class);
    assertEquals(test.findValue(ID1), Optional.of(MarketDataBox.ofSingleValue(VAL1)));
    assertEquals(test.findValue(ID2), Optional.of(MarketDataBox.ofSingleValue(VAL2)));
    assertEquals(test.findValue(ID3), Optional.empty());
    assertEquals(test.getIds(), ImmutableSet.of(ID1, ID2));
    assertEquals(test.findIds(ID1.getMarketDataName()), ImmutableSet.of(ID1));
    assertEquals(test.getTimeSeries(ID4), TIME_SERIES);
  }

  public void test_scenarios() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    assertEquals(test.scenarios().count(), 2);
    test.scenarios().forEach(md -> assertSame(md, BASE_DATA));
  }

  public void test_scenario_byIndex() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    assertSame(test.scenario(0), BASE_DATA);
    assertSame(test.scenario(1), BASE_DATA);
    assertThrows(() -> test.scenario(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.scenario(2), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    coverImmutableBean(test);
    RepeatedScenarioMarketData test2 = RepeatedScenarioMarketData.of(1, baseData2());
    coverBeanEquals(test, test2);
  }

  public void serialization() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  private static ImmutableMarketData baseData() {
    return ImmutableMarketData.builder(VAL_DATE)
        .addValue(ID1, VAL1)
        .addValue(ID2, VAL2)
        .addTimeSeriesMap(ImmutableMap.of(ID4, TIME_SERIES))
        .build();
  }

  private static ImmutableMarketData baseData2() {
    return ImmutableMarketData.builder(VAL_DATE)
        .addValue(ID1, VAL1)
        .addTimeSeriesMap(ImmutableMap.of(ID4, TIME_SERIES))
        .build();
  }

}
