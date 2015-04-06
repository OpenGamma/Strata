/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.builders;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;
import java.util.Set;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;

/**
 * Market data builder that creates failures with helpful error messages when there is no
 * mapping for an item of market data requested by a calculation.
 */
public final class MissingMappingMarketDataBuilder implements MarketDataBuilder<Void, MissingMappingId> {

  /** The single shared instance of the class. */
  public static final MissingMappingMarketDataBuilder INSTANCE = new MissingMappingMarketDataBuilder();

  // This class has no state and therefore the same instance can be shared
  private MissingMappingMarketDataBuilder() {
  }

  @Override
  public MarketDataRequirements requirements(MissingMappingId id) {
    return MarketDataRequirements.EMPTY;
  }

  @Override
  public Map<MissingMappingId, Result<Void>> build(Set<MissingMappingId> requirements, BaseMarketData builtData) {
    return requirements.stream().collect(toImmutableMap(id -> id, this::createResult));
  }

  /**
   * Returns a failure result with an error message explaining there was no market data mapping for the key.
   *
   * @param id  an ID wrapping a market data key for which there was no market data mapping
   * @return a failure result with an error message explaining there was no market data mapping for the key
   */
  private Result<Void> createResult(MissingMappingId id) {
    return Result.failure(
        FailureReason.MISSING_DATA,
        "No market data mapping found for market data key {}",
        id.getKey());
  }

  @Override
  public Class<MissingMappingId> getMarketDataIdType() {
    return MissingMappingId.class;
  }
}
