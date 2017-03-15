/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Objects;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;

@Test
public class CombinedScenarioMarketDataTest {

  private static final TestId TEST_ID1 = new TestId("1");
  private static final TestId TEST_ID2 = new TestId("2");
  private static final TestId TEST_ID3 = new TestId("3");

  public void test_combinedWith() {
    LocalDateDoubleTimeSeries timeSeries1 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 1)
        .put(date(2011, 3, 9), 2)
        .put(date(2011, 3, 10), 3)
        .build();

    LocalDateDoubleTimeSeries timeSeries2 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 10)
        .put(date(2011, 3, 9), 20)
        .put(date(2011, 3, 10), 30)
        .build();

    LocalDateDoubleTimeSeries timeSeries2a = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 1000)
        .put(date(2011, 3, 9), 2000)
        .put(date(2011, 3, 10), 3000)
        .build();

    LocalDateDoubleTimeSeries timeSeries3 = LocalDateDoubleTimeSeries.builder()
        .put(date(2011, 3, 8), 100)
        .put(date(2011, 3, 9), 200)
        .put(date(2011, 3, 10), 300)
        .build();

    ImmutableScenarioMarketData marketData1 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addTimeSeries(TEST_ID1, timeSeries1)
        .addTimeSeries(TEST_ID2, timeSeries2)
        .addBox(TEST_ID1, MarketDataBox.ofScenarioValues(1.0, 1.1, 1.2))
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(2.0, 2.1, 2.2))
        .build();

    ImmutableScenarioMarketData marketData2 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 10))
        .addTimeSeries(TEST_ID2, timeSeries2a)
        .addTimeSeries(TEST_ID3, timeSeries3)
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(21.0, 21.1, 21.2))
        .addBox(TEST_ID3, MarketDataBox.ofScenarioValues(3.0, 3.1, 3.2))
        .build();

    // marketData1 values should be in the combined data when the same ID is present in both
    ImmutableScenarioMarketData expected = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addTimeSeries(TEST_ID1, timeSeries1)
        .addTimeSeries(TEST_ID2, timeSeries2)
        .addTimeSeries(TEST_ID3, timeSeries3)
        .addBox(TEST_ID1, MarketDataBox.ofScenarioValues(1.0, 1.1, 1.2))
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(2.0, 2.1, 2.2))
        .addBox(TEST_ID3, MarketDataBox.ofScenarioValues(3.0, 3.1, 3.2))
        .build();

    ScenarioMarketData combined = marketData1.combinedWith(marketData2);
    assertThat(combined).isEqualTo(expected);
    assertThat(combined.getIds()).isEqualTo(ImmutableSet.of(TEST_ID1, TEST_ID2, TEST_ID3));
  }

  public void test_combinedWithIncompatibleScenarioCount() {
    ImmutableScenarioMarketData marketData1 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID1, MarketDataBox.ofScenarioValues(1.0, 1.1, 1.2))
        .build();

    ImmutableScenarioMarketData marketData2 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    assertThrowsIllegalArg(() -> marketData1.combinedWith(marketData2), ".* same number of scenarios .* 3 and 2");
  }

  public void test_combinedWithReceiverHasOneScenario() {
    ImmutableScenarioMarketData marketData1 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID1, MarketDataBox.ofSingleValue(1.0))
        .build();

    ImmutableScenarioMarketData marketData2 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    ImmutableScenarioMarketData expected = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID1, MarketDataBox.ofSingleValue(1.0))
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    ScenarioMarketData combined = marketData1.combinedWith(marketData2);
    assertThat(combined).isEqualTo(expected);
    assertThat(combined.getIds()).isEqualTo(ImmutableSet.of(TEST_ID1, TEST_ID2));
  }

  public void test_combinedWithOtherHasOneScenario() {
    ImmutableScenarioMarketData marketData1 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    ImmutableScenarioMarketData marketData2 = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID1, MarketDataBox.ofSingleValue(1.0))
        .build();

    ImmutableScenarioMarketData expected = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(TEST_ID1, MarketDataBox.ofSingleValue(1.0))
        .addBox(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    ScenarioMarketData combined = marketData1.combinedWith(marketData2);
    assertThat(combined).isEqualTo(expected);
    assertThat(combined.getIds()).isEqualTo(ImmutableSet.of(TEST_ID1, TEST_ID2));
  }

  //-------------------------------------------------------------------------
  private static final class TestId implements ObservableId {

    private final String id;

    private TestId(String id) {
      this.id = id;
    }

    @Override
    public StandardId getStandardId() {
      throw new UnsupportedOperationException("getStandardId not implemented");
    }

    @Override
    public FieldName getFieldName() {
      throw new UnsupportedOperationException("getFieldName not implemented");
    }

    @Override
    public ObservableSource getObservableSource() {
      throw new UnsupportedOperationException("getObservableSource not implemented");
    }

    @Override
    public ObservableId withObservableSource(ObservableSource obsSource) {
      throw new UnsupportedOperationException("withObservableSource not implemented");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestId testId = (TestId) o;
      return Objects.equals(id, testId.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

}
