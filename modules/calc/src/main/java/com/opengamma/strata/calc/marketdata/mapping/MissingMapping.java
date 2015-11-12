/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.mapping;

import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.marketdata.function.MissingMappingMarketDataFunction;
import com.opengamma.strata.calc.runner.MissingMappingId;

/**
 * Market data mapping implementation used when there is no mapping for a key.
 * <p>
 * It returns an ID indicating there was no mapping available for the key. This ID is handled by
 * {@link MissingMappingMarketDataFunction} which creates a failure result for the data with an
 * error message explaining the problem.
 * <p>
 * This approach always allows configuration to be built for a set of calculations - if the market data
 * configuration is incomplete, IDs will still be generated for all the requirements. When those
 * IDs are processed by the market data factory, they will appear as failures in the built market
 * data with an error message explaining the problem.
 */
public final class MissingMapping implements MarketDataMapping<Void, MarketDataKey<Void>> {

  /** Singleton instance. */
  public static final MissingMapping INSTANCE = new MissingMapping();

  // This class has no state so there is no need to create multiple instances
  private MissingMapping() {
  }

  /**
   * Throws {@code UnsupportedOperationException} as this method should never be called.
   * <p>
   * This is used to check that market data values are of the expected type. This class should
   * never be used to key any market data so this method won't ever be called.
   *
   * @return never returns
   * @throws UnsupportedOperationException always
   */
  @Override
  public Class<? extends MarketDataKey<Void>> getMarketDataKeyType() {
    throw new UnsupportedOperationException("getMarketDataIdType not supported");
  }

  @Override
  public MissingMappingId getIdForKey(MarketDataKey<Void> key) {
    return MissingMappingId.of(key);
  }
}
