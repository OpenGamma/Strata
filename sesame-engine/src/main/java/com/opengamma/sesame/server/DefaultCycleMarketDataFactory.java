/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.sesame.marketdata.StrategyAwareMarketDataSource;
import com.opengamma.util.ArgumentChecker;

/**
 * The default cycle market data factory, responsible for
 * providing market data based on a MarketDataFactory and
 * a primary market data source.
 */
public class DefaultCycleMarketDataFactory implements CycleMarketDataFactory {

  private final MarketDataFactory _marketDataFactory;
  private final StrategyAwareMarketDataSource _primaryMarketDataSource;

  /**
   * Create the factory for the view cycle.
   *
   * @param marketDataFactory  the market data factory to be used if
   * a different market data source is required during a cycle
   * @param primaryMarketDataSource  the primary market data source
   * to be used during the cycle
   */
  public DefaultCycleMarketDataFactory(MarketDataFactory marketDataFactory,
                                       StrategyAwareMarketDataSource primaryMarketDataSource) {
    _marketDataFactory = ArgumentChecker.notNull(marketDataFactory, "marketDataFactory");
    _primaryMarketDataSource = ArgumentChecker.notNull(primaryMarketDataSource, "primaryMarketDataSource");
  }

  /**
   * Create the factory for the view cycle.
   *
   * @param marketDataFactory  the market data factory to be used if
   * a different market data source is required during a cycle
   * @param marketDataSpec  the specification used to create the primary
   * market data source for the cycle
   */
  public DefaultCycleMarketDataFactory(MarketDataFactory marketDataFactory, MarketDataSpecification marketDataSpec) {
    this(marketDataFactory, marketDataFactory.create(marketDataSpec));
  }

  @Override
  public MarketDataSource getPrimaryMarketDataSource() {
    return _primaryMarketDataSource;
  }

  @Override
  public MarketDataSource getMarketDataSourceForDate(ZonedDateTime valuationTime) {
    return _marketDataFactory.create(new FixedHistoricalMarketDataSpecification(valuationTime.toLocalDate()));
  }

  @Override
  public CycleMarketDataFactory withMarketDataSpecification(MarketDataSpecification marketDataSpec) {
    return new DefaultCycleMarketDataFactory(_marketDataFactory, marketDataSpec);
  }

  @Override
  public CycleMarketDataFactory withPrimedMarketDataSource() {
    return new DefaultCycleMarketDataFactory(_marketDataFactory, _primaryMarketDataSource.createPrimedSource());
  }
}
