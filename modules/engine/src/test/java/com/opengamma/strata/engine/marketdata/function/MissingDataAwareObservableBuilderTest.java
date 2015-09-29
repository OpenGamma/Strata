/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.function;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.TestObservableId;
import com.opengamma.strata.collect.result.Result;

@Test
public class MissingDataAwareObservableBuilderTest {

  private static final MarketDataFeed VENDOR = MarketDataFeed.of("RealVendor");
  private static final TestObservableId ID1 = TestObservableId.of("1", VENDOR);
  private static final TestObservableId ID2 = TestObservableId.of("2", VENDOR);

  /**
   * Tests that failures are built for IDs with no market data feed and the delegate builder is used
   * for all others.
   */
  public void buildFailuresWhenNoMarketDataRule() {
    MissingDataAwareObservableFunction builder = new MissingDataAwareObservableFunction(new TestObservableFunction());
    TestObservableId id3 = TestObservableId.of("3", MarketDataFeed.NO_RULE);
    TestObservableId id4 = TestObservableId.of("4", MarketDataFeed.NO_RULE);
    ImmutableSet<TestObservableId> requirements = ImmutableSet.of(id3, id4, ID1, ID2);
    Map<ObservableId, Result<Double>> marketData = builder.build(requirements);

    assertThat(marketData.get(ID1)).hasValue(1d);
    assertThat(marketData.get(ID2)).hasValue(3d);
    assertThat(marketData.get(id3)).hasFailureMessageMatching("No market data rule.*");
    assertThat(marketData.get(id4)).hasFailureMessageMatching("No market data rule.*");
  }

  private static final class TestObservableFunction implements ObservableMarketDataFunction {

    private final Map<ObservableId, Result<Double>> marketData =
        ImmutableMap.of(
            ID1, Result.success(1d),
            ID2, Result.success(3d));

    @Override
    public Map<ObservableId, Result<Double>> build(Set<? extends ObservableId> requirements) {
      return requirements.stream()
          .filter(marketData::containsKey)
          .collect(toImmutableMap(id -> id, marketData::get));
    }
  }
}
