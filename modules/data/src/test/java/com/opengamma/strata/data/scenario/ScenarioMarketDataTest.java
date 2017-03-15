/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;

/**
 * Test {@link ScenarioMarketData} and {@link ImmutableScenarioMarketData}.
 */
@Test
public class ScenarioMarketDataTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final TestObservableId ID1 = TestObservableId.of("1");
  private static final TestObservableId ID2 = TestObservableId.of("2");
  private static final double VAL1 = 1d;
  private static final double VAL2 = 2d;
  private static final double VAL3 = 3d;
  private static final MarketDataBox<Double> BOX1 = MarketDataBox.ofScenarioValues(VAL1, VAL2);
  private static final MarketDataBox<Double> BOX2 = MarketDataBox.ofScenarioValues(VAL3);
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();

  //-------------------------------------------------------------------------
  public void test_of() {
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap = ImmutableMap.of(ID1, BOX1);
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID1, TIME_SERIES);
    ScenarioMarketData test = ScenarioMarketData.of(2, VAL_DATE, dataMap, tsMap);
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID1)).isTrue();
    assertThat(test.containsValue(ID2)).isFalse();
    assertThat(test.getValue(ID1)).isEqualTo(BOX1);
    assertThrows(() -> test.getValue(ID2), MarketDataNotFoundException.class);
    assertThat(test.findValue(ID1)).hasValue(BOX1);
    assertThat(test.findValue(ID2)).isEmpty();
    assertThat(test.getIds()).isEqualTo(ImmutableSet.of(ID1));
    assertThat(test.getTimeSeries(ID1)).isEqualTo(TIME_SERIES);
    assertThat(test.getTimeSeries(ID2)).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  public void test_of_noScenarios() {
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap = ImmutableMap.of(ID1, MarketDataBox.empty());
    ScenarioMarketData test = ScenarioMarketData.of(0, VAL_DATE, dataMap, ImmutableMap.of());
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID1)).isTrue();
    assertThat(test.containsValue(ID2)).isFalse();
    assertThat(test.getValue(ID1)).isEqualTo(MarketDataBox.empty());
    assertThrows(() -> test.getValue(ID2), MarketDataNotFoundException.class);
    assertThat(test.findValue(ID1)).hasValue(MarketDataBox.empty());
    assertThat(test.findValue(ID2)).isEmpty();
    assertThat(test.getIds()).isEqualTo(ImmutableSet.of(ID1));
    assertThat(test.getTimeSeries(ID1)).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(test.getTimeSeries(ID2)).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  public void test_of_repeated() {
    ScenarioMarketData test = ScenarioMarketData.of(1, MarketData.of(VAL_DATE, ImmutableMap.of(ID1, VAL1)));
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.getValue(ID1)).isEqualTo(MarketDataBox.ofSingleValue(VAL1));
  }

  public void test_empty() {
    ScenarioMarketData test = ScenarioMarketData.empty();
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.empty());
    assertThat(test.containsValue(ID1)).isFalse();
    assertThat(test.containsValue(ID2)).isFalse();
    assertThrows(() -> test.getValue(ID1), MarketDataNotFoundException.class);
    assertThrows(() -> test.getValue(ID2), MarketDataNotFoundException.class);
    assertThat(test.findValue(ID1)).isEmpty();
    assertThat(test.findValue(ID2)).isEmpty();
    assertThat(test.getIds()).isEqualTo(ImmutableSet.of());
    assertThat(test.getTimeSeries(ID1)).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(test.getTimeSeries(ID2)).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  public void of_null() {
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap = new HashMap<>();
    dataMap.put(ID1, null);
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID1, TIME_SERIES);
    assertThrows(() -> ScenarioMarketData.of(2, VAL_DATE, dataMap, tsMap), IllegalArgumentException.class);
  }

  public void of_badType() {
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap = ImmutableMap.of(ID1, MarketDataBox.ofScenarioValues("", ""));
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID1, TIME_SERIES);
    assertThrows(() -> ScenarioMarketData.of(2, VAL_DATE, dataMap, tsMap), ClassCastException.class);
  }

  public void of_badScenarios() {
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap = ImmutableMap.of(ID1, MarketDataBox.ofScenarioValues(VAL1));
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID1, TIME_SERIES);
    assertThrows(() -> ScenarioMarketData.of(2, VAL_DATE, dataMap, tsMap), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_defaultMethods() {
    ScenarioMarketData test = new ScenarioMarketData() {

      @Override
      public MarketDataBox<LocalDate> getValuationDate() {
        return MarketDataBox.ofSingleValue(VAL_DATE);
      }

      @Override
      public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
        return LocalDateDoubleTimeSeries.empty();
      }

      @Override
      public int getScenarioCount() {
        return 2;
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
        return id.equals(ID1) ? Optional.of((MarketDataBox<T>) BOX1) : Optional.empty();
      }

      @Override
      public Set<MarketDataId<?>> getIds() {
        return ImmutableSet.of();
      }

      @Override
      public <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name) {
        return ImmutableSet.of();
      }

      @Override
      public Set<ObservableId> getTimeSeriesIds() {
        return ImmutableSet.of();
      }
    };
    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID1)).isTrue();
    assertThat(test.containsValue(ID2)).isFalse();
    assertThat(test.getValue(ID1)).isEqualTo(BOX1);
    assertThrows(() -> test.getValue(ID2), MarketDataNotFoundException.class);
    assertThat(test.findValue(ID1)).hasValue(BOX1);
    assertThat(test.findValue(ID2)).isEmpty();
  }

  //-------------------------------------------------------------------------
  public void test_scenarios() {
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap = ImmutableMap.of(ID1, BOX1);
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID1, TIME_SERIES);
    ScenarioMarketData test = ScenarioMarketData.of(2, VAL_DATE, dataMap, tsMap);

    MarketData scenario0 = test.scenario(0);
    MarketData scenario1 = test.scenario(1);
    assertThat(scenario0.getValue(ID1)).isEqualTo(BOX1.getValue(0));
    assertThat(scenario1.getValue(ID1)).isEqualTo(BOX1.getValue(1));
    List<Double> list = test.scenarios().map(s -> s.getValue(ID1)).collect(toImmutableList());
    assertThat(list.get(0)).isEqualTo(BOX1.getValue(0));
    assertThat(list.get(1)).isEqualTo(BOX1.getValue(1));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap = ImmutableMap.of(ID1, BOX1);
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap = ImmutableMap.of(ID1, TIME_SERIES);
    ImmutableScenarioMarketData test = ImmutableScenarioMarketData.of(2, VAL_DATE, dataMap, tsMap);
    coverImmutableBean(test);
    Map<MarketDataId<?>, MarketDataBox<?>> dataMap2 = ImmutableMap.of(ID2, BOX2);
    Map<ObservableId, LocalDateDoubleTimeSeries> tsMap2 = ImmutableMap.of(ID2, TIME_SERIES);
    ImmutableScenarioMarketData test2 = ImmutableScenarioMarketData.of(1, VAL_DATE.plusDays(1), dataMap2, tsMap2);
    coverBeanEquals(test, test2);
  }

  //-------------------------------------------------------------------------
  public void getScenarioValueFromSingleValue() {
    MarketDataBox<Double> box = MarketDataBox.ofSingleValue(9d);
    TestMarketData marketData = new TestMarketData(box);
    TestArrayKey key = new TestArrayKey();
    TestDoubleArray array = marketData.getScenarioValue(key);
    assertThat(array.values).isEqualTo(DoubleArray.of(9, 9, 9));
  }

  public void getScenarioValueFromRequestedScenarioValue() {
    MarketDataBox<Double> box = MarketDataBox.ofScenarioValue(new TestDoubleArray(DoubleArray.of(9d, 9d, 9d)));
    TestMarketData marketData = new TestMarketData(box);
    TestArrayKey key = new TestArrayKey();
    TestDoubleArray array = marketData.getScenarioValue(key);
    assertThat(array.values).isEqualTo(DoubleArray.of(9, 9, 9));
  }

  public void getScenarioValueFromOtherScenarioValue() {
    MarketDataBox<Double> box = MarketDataBox.ofScenarioValues(9d, 9d, 9d);
    TestMarketData marketData = new TestMarketData(box);
    TestArrayKey key = new TestArrayKey();
    TestDoubleArray array = marketData.getScenarioValue(key);
    assertThat(array.values).isEqualTo(DoubleArray.of(9, 9, 9));
  }

  //--------------------------------------------------------------------------------------------------

  private static final class TestDoubleArray implements ScenarioArray<Double> {

    private final DoubleArray values;

    private TestDoubleArray(DoubleArray values) {
      this.values = values;
    }

    @Override
    public Double get(int scenarioIndex) {
      return values.get(scenarioIndex);
    }

    @Override
    public int getScenarioCount() {
      return values.size();
    }

    @Override
    public Stream<Double> stream() {
      return values.stream().boxed();
    }
  }

  //--------------------------------------------------------------------------------------------------

  private static final class TestId implements MarketDataId<Double> {

    @Override
    public Class<Double> getMarketDataType() {
      return Double.class;
    }
  }

  //--------------------------------------------------------------------------------------------------

  private static final class TestArrayKey implements ScenarioMarketDataId<Double, TestDoubleArray> {

    @Override
    public MarketDataId<Double> getMarketDataId() {
      return new TestId();
    }

    @Override
    public Class<TestDoubleArray> getScenarioMarketDataType() {
      return TestDoubleArray.class;
    }

    @Override
    public TestDoubleArray createScenarioValue(MarketDataBox<Double> marketDataBox, int scenarioCount) {
      return new TestDoubleArray(DoubleArray.of(scenarioCount, i -> marketDataBox.getValue(i)));
    }
  }

  //--------------------------------------------------------------------------------------------------

  private static final class TestMarketData implements ScenarioMarketData {

    private final MarketDataBox<?> value;

    private TestMarketData(MarketDataBox<?> value) {
      this.value = value;
    }

    @Override
    public MarketDataBox<LocalDate> getValuationDate() {
      throw new UnsupportedOperationException("getValuationDate() not implemented");
    }

    @Override
    public int getScenarioCount() {
      return 3;
    }

    @Override
    public Stream<MarketData> scenarios() {
      throw new UnsupportedOperationException("scenarios() not implemented");
    }

    @Override
    public MarketData scenario(int scenarioIndex) {
      throw new UnsupportedOperationException("scenario(int) not implemented");
    }

    @Override
    public boolean containsValue(MarketDataId<?> id) {
      throw new UnsupportedOperationException("containsValue(MarketDataKey) not implemented");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
      return (MarketDataBox<T>) value;
    }

    @Override
    public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
      throw new UnsupportedOperationException("findValue not implemented");
    }

    @Override
    public Set<MarketDataId<?>> getIds() {
      return ImmutableSet.of();
    }

    @Override
    public <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name) {
      return ImmutableSet.of();
    }

    @Override
    public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
      throw new UnsupportedOperationException("getTimeSeries(ObservableKey) not implemented");
    }

    @Override
    public Set<ObservableId> getTimeSeriesIds() {
      return ImmutableSet.of();
    }
  }
}
