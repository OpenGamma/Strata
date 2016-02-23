/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Objects;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.TestObservableId;
import com.opengamma.strata.basics.market.TestObservableKey;
import com.opengamma.strata.calc.runner.MissingMappingId;
import com.opengamma.strata.calc.runner.NoMatchingRuleId;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

@Test
public class MarketEnvironmentTest {

  private static final TestId TEST_ID1 = new TestId("1");
  private static final TestId TEST_ID2 = new TestId("2");
  private static final TestId TEST_ID3 = new TestId("3");
  private static final TestId TEST_ID4 = new TestId("4");

  /**
   * Tests the special handling of {@link NoMatchingRuleId}
   */
  public void handleNoMatchingRulesId() {
    MarketEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();
    NoMatchingRuleId id = NoMatchingRuleId.of(TestObservableKey.of("1"));
    String msgRegex = "No market data rules were available to build the market data for.*";
    assertThrows(() -> marketData.getValue(id), IllegalArgumentException.class, msgRegex);
  }

  /**
   * Tests the special handling of {@link MissingMappingId}
   */
  public void handleMissingMappingsId() {
    MarketEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();
    MissingMappingId id = MissingMappingId.of(TestObservableKey.of("1"));
    String msgRegex = "No market data mapping found for.*";
    assertThrows(() -> marketData.getValue(id), IllegalArgumentException.class, msgRegex);
  }

  /**
   * Tests the exception when there is a failure for an item of market data.
   */
  public void failureException() {
    TestObservableId id = TestObservableId.of("1");
    String failureMessage = "Something went wrong";
    MarketEnvironment marketData = MarketEnvironment
        .builder()
        .valuationDate(date(2011, 3, 8))
        .addResult(id, Result.failure(FailureReason.ERROR, failureMessage))
        .build();

    assertThrows(() -> marketData.getValue(id), FailureException.class, failureMessage);
  }

  public void filter() {
    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(TEST_ID1)
        .addTimeSeries(TEST_ID2)
        .build();

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

    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addTimeSeries(TEST_ID1, timeSeries1)
        .addTimeSeries(TEST_ID2, timeSeries2)
        .addValue(TEST_ID1, 1d)
        .addValue(TEST_ID2, 2d)
        .build()
        .filter(requirements);

    assertThat(marketData.containsValue(TEST_ID1)).isTrue();
    assertThat(marketData.containsValue(TEST_ID2)).isFalse();
    assertThat(marketData.findValue(TEST_ID1)).isPresent();
    assertThat(marketData.findValue(TEST_ID2)).isEmpty();
    assertThat(marketData.getValue(TEST_ID1).getSingleValue()).isEqualTo(1d);
    assertThat(marketData.getTimeSeries(TEST_ID2)).isEqualTo(timeSeries2);
  }

  public void valuationDateRequired() {
    assertThrowsIllegalArg(() -> MarketEnvironment.builder().build(), "Valuation date must be specified");
  }

  public void mergedWith() {
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

    MarketEnvironment marketData1 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addTimeSeries(TEST_ID1, timeSeries1)
        .addTimeSeries(TEST_ID2, timeSeries2)
        .addValue(TEST_ID1, MarketDataBox.ofScenarioValues(1.0, 1.1, 1.2))
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(2.0, 2.1, 2.2))
        .addResult(TEST_ID4, Result.failure(FailureReason.ERROR, "foo"))
        .build();

    MarketEnvironment marketData2 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 10))
        .addTimeSeries(TEST_ID2, timeSeries2a)
        .addTimeSeries(TEST_ID3, timeSeries3)
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(21.0, 21.1, 21.2))
        .addValue(TEST_ID3, MarketDataBox.ofScenarioValues(3.0, 3.1, 3.2))
        .addTimeSeriesResult(TEST_ID4, Result.failure(FailureReason.ERROR, "foo"))
        .build();

    // marketData1 values should be in the merged data when the same ID is present in both
    MarketEnvironment expected = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addTimeSeries(TEST_ID1, timeSeries1)
        .addTimeSeries(TEST_ID2, timeSeries2)
        .addTimeSeries(TEST_ID3, timeSeries3)
        .addValue(TEST_ID1, MarketDataBox.ofScenarioValues(1.0, 1.1, 1.2))
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(2.0, 2.1, 2.2))
        .addValue(TEST_ID3, MarketDataBox.ofScenarioValues(3.0, 3.1, 3.2))
        .build();

    MarketEnvironment merged = marketData1.mergedWith(marketData2);
    assertThat(merged).isEqualTo(expected);
  }

  public void mergedWithIncompatibleScenarioCount() {
    MarketEnvironment marketData1 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID1, MarketDataBox.ofScenarioValues(1.0, 1.1, 1.2))
        .build();

    MarketEnvironment marketData2 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    assertThrowsIllegalArg(() -> marketData1.mergedWith(marketData2), ".* same number of scenarios .* 3 and 2");
  }

  public void mergedWithReceiverHasOneScenario() {
    MarketEnvironment marketData1 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID1, MarketDataBox.ofScenarioValues(1.0))
        .build();

    MarketEnvironment marketData2 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    MarketEnvironment expected = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID1, MarketDataBox.ofScenarioValues(1.0))
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    assertThat(marketData1.mergedWith(marketData2)).isEqualTo(expected);
  }

  public void mergedWithOtherHasOneScenario() {
    MarketEnvironment marketData1 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    MarketEnvironment marketData2 = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID1, MarketDataBox.ofScenarioValues(1.0))
        .build();

    MarketEnvironment expected = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(TEST_ID1, MarketDataBox.ofScenarioValues(1.0))
        .addValue(TEST_ID2, MarketDataBox.ofScenarioValues(1.0, 1.1))
        .build();

    assertThat(marketData1.mergedWith(marketData2)).isEqualTo(expected);
  }

  /**
   * Test that an FxRate with a rate of 1 is returned if a rate is requested for a pair containing the same
   * currency twice, e.g. GBP/GBP.
   */
  public void identityFxRate() {
    MarketDataBox<FxRate> box = MarketEnvironment.empty().getValue(FxRateId.of(Currency.GBP, Currency.GBP));
    assertThat(box.isSingleValue()).isTrue();
    FxRate rate = box.getValue(0);
    assertThat(rate.fxRate(Currency.GBP, Currency.GBP)).isEqualTo(1d);
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
    public MarketDataFeed getMarketDataFeed() {
      throw new UnsupportedOperationException("getMarketDataFeed not implemented");
    }

    @Override
    public ObservableKey toMarketDataKey() {
      throw new UnsupportedOperationException("toMarketDataKey not implemented");
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
