/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.marketdata.spec.UserMarketDataSpecification;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class SnapshotMarketDataFactory implements MarketDataFactory {

  private final MarketDataSnapshotSource _snapshotSource;

  public SnapshotMarketDataFactory(MarketDataSnapshotSource snapshotSource) {
    _snapshotSource = ArgumentChecker.notNull(snapshotSource, "snapshotSource");
  }

  @Override
  public StrategyAwareMarketDataSource create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof UserMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + UserMarketDataSpecification.class + " but was " + spec.getClass());
    }
    UserMarketDataSpecification snapshotMarketDataSpec = (UserMarketDataSpecification) spec;
    SnapshotMarketDataSource snapshotSource = new SnapshotMarketDataSource(_snapshotSource, snapshotMarketDataSpec.getUserSnapshotId());
    return new DefaultStrategyAwareMarketDataSource(spec, snapshotSource);
  }
}
