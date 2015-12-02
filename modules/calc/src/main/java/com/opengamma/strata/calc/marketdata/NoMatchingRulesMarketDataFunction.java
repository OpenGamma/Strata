/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.runner.NoMatchingRuleId;

/**
 * Market data function that creates failures with helpful error messages when the market
 * data rules don't match a calculation target and there are no market data mappings.
 * <p>
 * This builder uses raw types because it doesn't conform to the normal {@link MarketDataFunction}
 * contract. It is only used for building failure results, never market data.
 */
@SuppressWarnings("rawtypes")
public final class NoMatchingRulesMarketDataFunction implements MarketDataFunction {

  /** The single shared instance of the class. */
  static final NoMatchingRulesMarketDataFunction INSTANCE = new NoMatchingRulesMarketDataFunction();

  // This class has no state and therefore the same instance can be shared
  private NoMatchingRulesMarketDataFunction() {
  }

  @Override
  public MarketDataRequirements requirements(MarketDataId id, MarketDataConfig marketDataConfig) {
    return MarketDataRequirements.empty();
  }

  @SuppressWarnings("unchecked")
  @Override
  public MarketDataBox build(MarketDataId id, CalculationEnvironment marketData, MarketDataConfig marketDataConfig) {
    throw new IllegalArgumentException(
        "No market data rules were available to build the market data for key " + ((NoMatchingRuleId) id).getKey());
  }

  @Override
  public Class getMarketDataIdType() {
    return NoMatchingRuleId.class;
  }

}
