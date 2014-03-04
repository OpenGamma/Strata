/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.marketdata.spec.UserMarketDataSpecification;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.util.ArgumentChecker;

/**
 * Factory for {@link MarketDataFn} instances which use market data source from a user snapshot.
 */
public class UserSnapshotMarketDataFnFactory implements MarketDataFnFactory {

  private final MarketDataSnapshotSource _snapshotSource;
  private final String _currencyMatrixConfigName;

  public UserSnapshotMarketDataFnFactory(MarketDataSnapshotSource snapshotSource, String currencyMatrixConfigName) {
    _snapshotSource = ArgumentChecker.notNull(snapshotSource, "snapshotSource");
    _currencyMatrixConfigName = ArgumentChecker.notEmpty(currencyMatrixConfigName, "currencyMatrixConfigName");
  }
  
  @Override
  public MarketDataFn create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof UserMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + UserMarketDataSpecification.class + " but was " + spec.getClass());
    }
    UserMarketDataSpecification snapshotMarketDataSpec = (UserMarketDataSpecification) spec;
    RawMarketDataSource dataSource = new SnapshotMarketDataSource(_snapshotSource, snapshotMarketDataSpec.getUserSnapshotId());
    ConfigLink<CurrencyMatrix> configLink = ConfigLink.of(_currencyMatrixConfigName, CurrencyMatrix.class);
    return new EagerMarketDataFn(configLink.resolve(), dataSource);
  }
}
