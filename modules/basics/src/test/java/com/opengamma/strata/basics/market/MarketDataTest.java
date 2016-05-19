/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link MarketData} and {@link ImmutableMarketData}.
 */
@Test
public class MarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestObservableId ID1 = TestObservableId.of("1");
  private static final TestObservableId ID2 = TestObservableId.of("2");
  private static final TestObservableId ID3 = TestObservableId.of("3");
  private static final Double VAL1 = 1d;
  private static final Double VAL2 = 2d;
  private static final Double VAL3 = 3d;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();

  //-------------------------------------------------------------------------
  public void test_of_2arg() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    MarketData test = MarketData.of(VAL_DATE, dataMap);

    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));

    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.getValue(ID2), VAL2);
    assertEquals(test.findValue(ID2), Optional.of(VAL2));

    assertEquals(test.containsValue(ID3), false);
    assertThrows(() -> test.getValue(ID3), MarketDataNotFoundException.class);
    assertEquals(test.findValue(ID3), Optional.empty());

    assertEquals(test.getTimeSeries(ID1), LocalDateDoubleTimeSeries.empty());
    assertEquals(test.getTimeSeries(ID2), LocalDateDoubleTimeSeries.empty());
  }

  public void test_of_3arg() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID2, TIME_SERIES);
    MarketData test = MarketData.of(VAL_DATE, dataMap, tsMap);

    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));

    assertEquals(test.containsValue(ID2), false);
    assertThrows(() -> test.getValue(ID2), MarketDataNotFoundException.class);
    assertEquals(test.findValue(ID2), Optional.empty());

    assertEquals(test.getTimeSeries(ID1), LocalDateDoubleTimeSeries.empty());
    assertEquals(test.getTimeSeries(ID2), TIME_SERIES);
  }

  public void empty() {
    MarketData test = MarketData.empty(VAL_DATE);

    assertEquals(test.containsValue(ID1), false);
    assertEquals(test.getTimeSeries(ID1), LocalDateDoubleTimeSeries.empty());
  }

  //-------------------------------------------------------------------------
  public void test_builder() {
    ImmutableMarketData test = ImmutableMarketData.builder(VAL_DATE.plusDays(1))
        .valuationDate(VAL_DATE)
        .addValue(ID1, 123d)
        .addValues(ImmutableMap.of(ID3, 201d))
        .addTimeSeries(ID2, TIME_SERIES)
        .build();
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.getValues().get(ID1), 123d);
    assertEquals(test.getTimeSeries().get(ID2), TIME_SERIES);
  }

  public void test_of_badType() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, "123");
    assertThrows(() -> MarketData.of(VAL_DATE, dataMap), ClassCastException.class);
  }

  public void test_of_null() {
    Map<MarketDataId<?>, Object> dataMap = new HashMap<>();
    dataMap.put(ID1, null);
    assertThrowsIllegalArg(() -> MarketData.of(VAL_DATE, dataMap));
  }

  //-------------------------------------------------------------------------
  public void test_defaultMethods() {
    MarketData test = new MarketData() {

      @Override
      public LocalDate getValuationDate() {
        return VAL_DATE;
      }

      @Override
      public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
        return TIME_SERIES;
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> Optional<T> findValue(MarketDataId<T> id) {
        return id.equals(ID1) ? Optional.of((T) VAL1) : Optional.empty();
      }
    };
    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.containsValue(ID2), false);
    assertEquals(test.getValue(ID1), VAL1);
    assertThrows(() -> test.getValue(ID2), MarketDataNotFoundException.class);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));
    assertEquals(test.findValue(ID2), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_noClash() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    MarketData test2 = MarketData.of(VAL_DATE, dataMap2);

    MarketData test = test1.combinedWith(test2);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_noClashSame() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    MarketData test2 = MarketData.of(VAL_DATE, dataMap2);

    MarketData test = test1.combinedWith(test2);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_clash() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL3);
    MarketData test2 = MarketData.of(VAL_DATE, dataMap2);
    MarketData combined = test1.combinedWith(test2);
    assertEquals(combined.getValue(ID1), VAL1);
  }

  public void test_combinedWith_dateMismatch() {
    Map<MarketDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    MarketData test1 = MarketData.of(VAL_DATE, dataMap1);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL3);
    MarketData test2 = MarketData.of(VAL_DATE.plusDays(1), dataMap2);
    assertThrowsIllegalArg(() -> test1.combinedWith(test2));
  }

  //-------------------------------------------------------------------------
  public void test_withValue() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    MarketData test = MarketData.of(VAL_DATE, dataMap).withValue(ID1, VAL3);
    assertEquals(test.getValue(ID1), VAL3);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ImmutableMarketData test = ImmutableMarketData.of(VAL_DATE, dataMap);
    coverImmutableBean(test);
    Map<MarketDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableMarketData test2 = ImmutableMarketData.of(VAL_DATE.minusDays(1), dataMap2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    Map<MarketDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    MarketData test = MarketData.of(VAL_DATE, dataMap);
    assertSerialization(test);
  }

}
