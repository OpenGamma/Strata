/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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
  @Test
  public void of_addition() {
    ExtendedScenarioMarketData<String> test = ExtendedScenarioMarketData.of(ID3, VAL3, BASE_DATA);
    assertThat(test.getId()).isEqualTo(ID3);
    assertThat(test.getValue()).isEqualTo(VAL3);
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.containsValue(ID2)).isEqualTo(true);
    assertThat(test.containsValue(ID3)).isEqualTo(true);
    assertThat(test.containsValue(ID4)).isEqualTo(false);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThat(test.getValue(ID3)).isEqualTo(VAL3);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID4));
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL1));
    assertThat(test.findValue(ID2)).isEqualTo(Optional.of(VAL2));
    assertThat(test.findValue(ID3)).isEqualTo(Optional.of(VAL3));
    assertThat(test.findValue(ID4)).isEqualTo(Optional.empty());
    assertThat(test.getIds()).containsExactlyInAnyOrder(ID1, ID2, ID3);
    assertThat(test.findIds(ID1.getMarketDataName())).isEqualTo(ImmutableSet.of(ID1));
    assertThat(test.findIds(ID3.getMarketDataName())).isEqualTo(ImmutableSet.of(ID3));
    assertThat(test.getTimeSeries(ID4)).isEqualTo(TIME_SERIES);
  }

  @Test
  public void of_override() {
    ExtendedScenarioMarketData<String> test = ExtendedScenarioMarketData.of(ID1, VAL3, BASE_DATA);
    assertThat(test.getId()).isEqualTo(ID1);
    assertThat(test.getValue()).isEqualTo(VAL3);
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.containsValue(ID2)).isEqualTo(true);
    assertThat(test.containsValue(ID3)).isEqualTo(false);
    assertThat(test.getValue(ID1)).isEqualTo(VAL3);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID3));
    assertThat(test.getIds()).containsExactlyInAnyOrder(ID1, ID2);
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL3));
    assertThat(test.findValue(ID2)).isEqualTo(Optional.of(VAL2));
    assertThat(test.findValue(ID3)).isEqualTo(Optional.empty());
    assertThat(test.getTimeSeries(ID4)).isEqualTo(TIME_SERIES);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ExtendedScenarioMarketData<String> test = ExtendedScenarioMarketData.of(ID1, VAL1, BASE_DATA);
    coverImmutableBean(test);
    ExtendedScenarioMarketData<String> test2 = ExtendedScenarioMarketData.of(
        ID2,
        VAL2,
        ImmutableScenarioMarketData.of(3, VAL_DATE, ImmutableMap.of(), ImmutableMap.of()));
    coverBeanEquals(test, test2);
  }

  @Test
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
