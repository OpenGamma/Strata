/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.builders;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.marketdata.id.IndexRateId;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.ObservableId;

@Test
public class MissingDataAwareObservableBuilderTest {

  private static final MarketDataFeed VENDOR = MarketDataFeed.of("RealVendor");
  private static final IndexRateId LIBOR_1M_ID = IndexRateId.of(IborIndices.USD_LIBOR_1M, VENDOR);
  private static final IndexRateId LIBOR_3M_ID = IndexRateId.of(IborIndices.USD_LIBOR_3M, VENDOR);

  /**
   * Tests that failures are built for IDs with no market data feed and the delegate builder is used
   * for all others.
   */
  public void buildFailuresWhenNoMarketDataRule() {
    MissingDataAwareObservableBuilder builder = new MissingDataAwareObservableBuilder(new DelegateBuilder());
    IndexRateId libor6mId = IndexRateId.of(IborIndices.USD_LIBOR_6M, MarketDataFeed.NO_RULE);
    IndexRateId libor12mId = IndexRateId.of(IborIndices.USD_LIBOR_12M, MarketDataFeed.NO_RULE);
    ImmutableSet<IndexRateId> requirements = ImmutableSet.of(libor6mId, libor12mId, LIBOR_1M_ID, LIBOR_3M_ID);
    Map<ObservableId, Result<Double>> marketData = builder.build(requirements);

    assertThat(marketData.get(LIBOR_1M_ID)).hasValue(1d);
    assertThat(marketData.get(LIBOR_3M_ID)).hasValue(3d);
    assertThat(marketData.get(libor6mId)).hasFailureMessageMatching("No market data rule.*");
    assertThat(marketData.get(libor12mId)).hasFailureMessageMatching("No market data rule.*");
  }

  private static final class DelegateBuilder implements ObservableMarketDataBuilder {

    private final Map<ObservableId, Result<Double>> marketData =
        ImmutableMap.of(
            LIBOR_1M_ID, Result.success(1d),
            LIBOR_3M_ID, Result.success(3d));

    @Override
    public Map<ObservableId, Result<Double>> build(Set<? extends ObservableId> requirements) {
      return requirements.stream()
          .filter(marketData::containsKey)
          .collect(toImmutableMap(id -> id, marketData::get));
    }
  }
}
