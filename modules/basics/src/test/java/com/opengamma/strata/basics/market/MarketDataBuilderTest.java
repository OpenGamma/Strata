/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

@Test
public class MarketDataBuilderTest {

  private static final TestObservableKey KEY1 = TestObservableKey.of("1");
  private static final TestObservableKey KEY2 = TestObservableKey.of("2");
  private static final TestObservableKey KEY3 = TestObservableKey.of("3");
  private static final LocalDateDoubleTimeSeries TIME_SERIES1 = LocalDateDoubleTimeSeries.builder()
      .put(LocalDate.of(2011, 3, 8), 1.1)
      .put(LocalDate.of(2011, 3, 10), 1.2)
      .build();
  private static final LocalDateDoubleTimeSeries TIME_SERIES2 = LocalDateDoubleTimeSeries.builder()
      .put(LocalDate.of(2012, 3, 8), 2.1)
      .put(LocalDate.of(2012, 3, 10), 2.2)
      .build();

  public void addValue() {
    MarketData marketData = MarketData.builder().addValue(KEY1, 123d).build();
    assertThat(marketData.getValue(KEY1)).isEqualTo(123d);
    assertThat(marketData.containsValue(KEY2)).isFalse();
  }

  public void addValues() {
    Map<? extends MarketDataKey<?>, ?> marketDataMap = ImmutableMap.of(KEY1, 123d, KEY2, 321d);
    MarketData marketData = MarketData.of(marketDataMap);
    assertThat(marketData.getValue(KEY1)).isEqualTo(123d);
    assertThat(marketData.getValue(KEY2)).isEqualTo(321d);
    assertThat(marketData.containsValue(KEY3)).isFalse();
  }

  public void addTimeSeries() {
    MarketData marketData = MarketData.builder().addTimeSeries(KEY1, TIME_SERIES1).build();
    assertThat(marketData.getTimeSeries(KEY1)).isEqualTo(TIME_SERIES1);
    assertThat(marketData.containsTimeSeries(KEY2)).isFalse();
  }

  public void addTimeSeriesMap() {
    Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(KEY1, TIME_SERIES1, KEY2, TIME_SERIES2);
    MarketData marketData = MarketData.builder().addTimeSeries(timeSeriesMap).build();
    assertThat(marketData.getTimeSeries(KEY1)).isEqualTo(TIME_SERIES1);
    assertThat(marketData.getTimeSeries(KEY2)).isEqualTo(TIME_SERIES2);
    assertThat(marketData.containsTimeSeries(KEY3)).isFalse();
  }

  public void wrongType() {
    Map<? extends MarketDataKey<?>, ?> marketDataMap = ImmutableMap.of(KEY1, 123d, KEY2, "abc");
    assertThrowsIllegalArg(() -> MarketData.of(marketDataMap));
    assertThrowsIllegalArg(() -> MarketData.builder().addValues(marketDataMap));
  }
}
