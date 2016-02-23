/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ScenarioMarketDataKey;
import com.opengamma.strata.basics.market.ScenarioMarketDataValue;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

@Test
public class CalculationMarketDataTest {

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

  private static final class TestDoubleArray implements ScenarioMarketDataValue<Double> {

    private final DoubleArray values;

    private TestDoubleArray(DoubleArray values) {
      this.values = values;
    }

    @Override
    public Double getValue(int scenarioIndex) {
      return values.get(scenarioIndex);
    }

    @Override
    public int getScenarioCount() {
      return values.size();
    }
  }

  //--------------------------------------------------------------------------------------------------

  private static final class TestKey implements MarketDataKey<Double> {

    @Override
    public Class<Double> getMarketDataType() {
      return Double.class;
    }
  }

  //--------------------------------------------------------------------------------------------------

  private static final class TestArrayKey implements ScenarioMarketDataKey<Double, TestDoubleArray> {

    @Override
    public MarketDataKey<Double> getMarketDataKey() {
      return new TestKey();
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

  private static final class TestMarketData implements CalculationMarketData {

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
    public boolean containsValue(MarketDataKey<?> key) {
      throw new UnsupportedOperationException("containsValue(MarketDataKey) not implemented");
    }

    @Override
    public <T> Optional<MarketDataBox<T>> findValue(MarketDataKey<T> key) {
      throw new UnsupportedOperationException("findValue not implemented");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> MarketDataBox<T> getValue(MarketDataKey<T> key) {
      return (MarketDataBox<T>) value;
    }

    @Override
    public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
      throw new UnsupportedOperationException("getTimeSeries(ObservableKey) not implemented");
    }
  }
}
