/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link MarketData}.
 */
@Test
public class MarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestObservableKey KEY1 = TestObservableKey.of("1");
  private static final TestObservableKey KEY2 = TestObservableKey.of("2");
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();

  //-------------------------------------------------------------------------
  public void of_2arg() {
    Map<MarketDataKey<?>, Object> dataMap = ImmutableMap.of(KEY1, 123d);
    MarketData test = MarketData.of(VAL_DATE, dataMap);
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.containsValue(KEY1)).isTrue();
    assertThat(test.containsValue(KEY2)).isFalse();
    assertThat(test.findValue(KEY1)).isPresent();
    assertThat(test.findValue(KEY2)).isEmpty();
    assertThat(test.getValue(KEY1)).isEqualTo(123d);
  }

  public void of_3arg() {
    Map<MarketDataKey<?>, Object> dataMap = ImmutableMap.of(KEY1, 123d);
    Map<ObservableKey, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(KEY2, TIME_SERIES);
    MarketData test = MarketData.of(VAL_DATE, dataMap, tsMap);
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.containsValue(KEY1)).isTrue();
    assertThat(test.containsValue(KEY2)).isFalse();
    assertThat(test.findValue(KEY1)).isPresent();
    assertThat(test.findValue(KEY2)).isEmpty();
    assertThat(test.getValue(KEY1)).isEqualTo(123d);
    assertThat(test.getTimeSeries(KEY2)).isEqualTo(TIME_SERIES);
  }

}
