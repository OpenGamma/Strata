/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.util.Optional;

import org.junit.jupiter.api.Test;

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
  @Test
  public void test_of() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    assertThat(test.getScenarioCount()).isEqualTo(2);
    assertThat(test.getUnderlying()).isEqualTo(BASE_DATA);
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.containsValue(ID2)).isEqualTo(true);
    assertThat(test.containsValue(ID3)).isEqualTo(false);
    assertThat(test.getValue(ID1)).isEqualTo(MarketDataBox.ofSingleValue(VAL1));
    assertThat(test.getValue(ID2)).isEqualTo(MarketDataBox.ofSingleValue(VAL2));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID3));
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(MarketDataBox.ofSingleValue(VAL1)));
    assertThat(test.findValue(ID2)).isEqualTo(Optional.of(MarketDataBox.ofSingleValue(VAL2)));
    assertThat(test.findValue(ID3)).isEqualTo(Optional.empty());
    assertThat(test.getIds()).containsExactlyInAnyOrder(ID1, ID2);
    assertThat(test.findIds(ID1.getMarketDataName())).isEqualTo(ImmutableSet.of(ID1));
    assertThat(test.getTimeSeries(ID4)).isEqualTo(TIME_SERIES);
  }

  @Test
  public void test_scenarios() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    assertThat(test.scenarios().count()).isEqualTo(2);
    test.scenarios().forEach(md -> assertThat(md).isSameAs(BASE_DATA));
  }

  @Test
  public void test_scenario_byIndex() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    assertThat(test.scenario(0)).isSameAs(BASE_DATA);
    assertThat(test.scenario(1)).isSameAs(BASE_DATA);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.scenario(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.scenario(2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RepeatedScenarioMarketData test = RepeatedScenarioMarketData.of(2, BASE_DATA);
    coverImmutableBean(test);
    RepeatedScenarioMarketData test2 = RepeatedScenarioMarketData.of(1, baseData2());
    coverBeanEquals(test, test2);
  }

  @Test
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
