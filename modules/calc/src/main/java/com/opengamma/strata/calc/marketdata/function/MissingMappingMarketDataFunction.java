/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.function;

import com.opengamma.strata.calc.marketdata.MarketDataLookup;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.calc.runner.MissingMappingId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Market data function that creates failures with helpful error messages when there is no
 * mapping for an item of market data requested by a calculation.
 */
public final class MissingMappingMarketDataFunction implements MarketDataFunction<Void, MissingMappingId> {

  /** The single shared instance of the class. */
  public static final MissingMappingMarketDataFunction INSTANCE = new MissingMappingMarketDataFunction();

  // This class has no state and therefore the same instance can be shared
  private MissingMappingMarketDataFunction() {
  }

  @Override
  public MarketDataRequirements requirements(MissingMappingId id, MarketDataConfig marketDataConfig) {
    return MarketDataRequirements.empty();
  }

  @Override
  public Result<MarketDataBox<Void>> build(
      MissingMappingId id,
      MarketDataLookup marketData,
      MarketDataConfig marketDataConfig) {

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
