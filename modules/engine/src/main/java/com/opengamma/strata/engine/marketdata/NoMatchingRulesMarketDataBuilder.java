/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.marketdata.MarketDataLookup;
import com.opengamma.strata.marketdata.id.MarketDataId;

/**
 * Market data builder that creates failures with helpful error messages when the market
 * data rules don't match a calculation target and there are no market data mappings.
 * <p>
 * This builder uses raw types because it doesn't conform to the normal {@link MarketDataBuilder}
 * contract. It is only used for building failure results, never market data.
 */
public final class NoMatchingRulesMarketDataBuilder implements MarketDataBuilder {

  /** The single shared instance of the class. */
  static final NoMatchingRulesMarketDataBuilder INSTANCE = new NoMatchingRulesMarketDataBuilder();

  // This class has no state and therefore the same instance can be shared
  private NoMatchingRulesMarketDataBuilder() {
  }

  @Override
  public MarketDataRequirements requirements(MarketDataId id) {
    return MarketDataRequirements.EMPTY;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result build(MarketDataId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
    return Result.failure(
        FailureReason.MISSING_DATA,
        "No market data rules were available to build the market data for key {}",
        ((NoMatchingRuleId) id).getKey());
  }

  @Override
  public Class getMarketDataIdType() {
    return NoMatchingRuleId.class;
  }

}
