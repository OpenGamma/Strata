/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link ImmutableMarketData}.
 */
@Test
public class ImmutableMarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestObservableKey KEY1 = TestObservableKey.of("1");
  private static final TestObservableKey KEY2 = TestObservableKey.of("2");
  private static final TestObservableKey KEY3 = TestObservableKey.of("3");
  private static final TestObservableId ID3 = TestObservableId.of("3");
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final ImmutableMarketData DATA = data();

  //-------------------------------------------------------------------------
  public void of() {
    Map<MarketDataKey<?>, Object> dataMap = ImmutableMap.of(KEY1, 123d);
    ImmutableMarketData test = ImmutableMarketData.of(VAL_DATE, dataMap);
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.getValues()).containsEntry(KEY1, 123d);
    assertThat(test.getTimeSeries()).isEmpty();
  }

  public void of_badType() {
    Map<MarketDataKey<?>, Object> dataMap = ImmutableMap.of(KEY1, "123");
    assertThrowsIllegalArg(() -> ImmutableMarketData.of(VAL_DATE, dataMap));
  }

  public void builder() {
    ImmutableMarketData test = ImmutableMarketData.builder(VAL_DATE.plusDays(1))
        .valuationDate(VAL_DATE)
        .addValue(KEY1, 123d)
        .addValuesById(ImmutableMap.of(ID3, 201d))
        .addTimeSeries(KEY2, TIME_SERIES)
        .build();
    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.getValues()).containsEntry(KEY1, 123d);
    assertThat(test.getValues()).containsEntry(KEY3, 201d);
    assertThat(test.getTimeSeries()).containsEntry(KEY2, TIME_SERIES);
  }

  public void containsValue() {
    assertThat(DATA.containsValue(KEY1)).isTrue();
    assertThat(DATA.containsValue(KEY2)).isFalse();
    assertThat(DATA.findValue(KEY1)).isPresent();
    assertThat(DATA.findValue(KEY2)).isEmpty();
  }

  public void getValue() {
    assertThat(DATA.getValue(KEY1)).isEqualTo(123d);
    assertThrowsIllegalArg(() -> DATA.getValue(KEY2));
  }

  public void getTimeSeries() {
    assertThat(DATA.getTimeSeries(KEY1)).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(DATA.getTimeSeries(KEY2)).isEqualTo(TIME_SERIES);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(DATA);
    Map<MarketDataKey<?>, Object> dataMap = ImmutableMap.of(KEY2, 123d);
    Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(KEY1, TIME_SERIES);
    ImmutableMarketData test2 =
        ImmutableMarketData.builder(VAL_DATE.plusDays(1)).values(dataMap).timeSeries(timeSeriesMap).build();
    coverBeanEquals(DATA, test2);
  }

  public void serialization() {
    assertSerialization(DATA);
  }

  private static ImmutableMarketData data() {
    Map<MarketDataKey<?>, Object> dataMap = ImmutableMap.of(KEY1, 123d);
    Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap = ImmutableMap.of(KEY2, TIME_SERIES);
    return ImmutableMarketData.builder(VAL_DATE).values(dataMap).timeSeries(timeSeriesMap).build();
  }

}
