/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.function;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Observable market data function that handles data that can't be built because there was
 * no market data rule for the calculation. It delegates to another builder for building data.
 * <p>
 * When there is no market data rule for a calculation, the {@link ObservableId} instances for the market
 * data have the feed {@link MarketDataFeed#NO_RULE}. This builder creates failure results for those IDs and
 * uses the delegate builder to build the data for the remaining IDs.
 */
public final class MissingDataAwareObservableFunction implements ObservableMarketDataFunction {

  /** Delegate builder used for building. */
  private final ObservableMarketDataFunction delegate;

  /**
   * @param delegate a builder for building observable market data
   */
  public MissingDataAwareObservableFunction(ObservableMarketDataFunction delegate) {
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public Map<ObservableId, Result<Double>> build(Set<? extends ObservableId> requirements) {
    // Results for IDs with a market data feed of NO_RULE.
    // These IDs can't be used to look up market data because there was no market data rule to specify the
    // market data feed.
    Map<ObservableId, Result<Double>> failures =
        requirements.stream()
            .filter(id -> id.getMarketDataFeed().equals(MarketDataFeed.NO_RULE))
            .collect(toImmutableMap(id -> id, this::createFailure));

    // The requirements that have a real market data feed and can be resolved into market data
    Set<? extends ObservableId> ids = Sets.difference(requirements, failures.keySet());
    Map<ObservableId, Result<Double>> marketData = delegate.build(ids);
    return ImmutableMap.<ObservableId, Result<Double>>builder()
        .putAll(failures)
        .putAll(marketData)
        .build();
  }

  private Result<Double> createFailure(ObservableId id) {
    return Result.failure(
        FailureReason.MISSING_DATA,
        "No market data rule specifying market data feed for {}",
        id);
  }
}
