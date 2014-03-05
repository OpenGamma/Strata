/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.ValuationTimeCacheEntry;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;

/**
 * {@link Environment} implementation created and managed by the engine.
 * This allows the engine to monitor what data from the environment is used by each function and invalidate cache
 * entries when the data changes.
 * This class is package-private because it is only intended to be used by the engine itself, not user code.
 */
/* package */ class EngineEnvironment implements Environment {

  private final Environment _delegate;
  private final CacheInvalidator _cacheInvalidator;

  /* package */ EngineEnvironment(ZonedDateTime valuationTime,
                                  MarketDataSource marketDataSource,
                                  CacheInvalidator cacheInvalidator) {
    _delegate = new SimpleEnvironment(ArgumentChecker.notNull(valuationTime, "valuationTime"),
                                      ArgumentChecker.notNull(marketDataSource, "marketDataSource"));
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
  }

  @Override
  public LocalDate getValuationDate() {
    LocalDate valuationDate = _delegate.getValuationDate();
    _cacheInvalidator.register(new ValuationTimeCacheEntry.ValidOnCalculationDay(valuationDate));
    return valuationDate;
  }

  @Override
  public ZonedDateTime getValuationTime() {
    ZonedDateTime valuationTime = _delegate.getValuationTime();
    _cacheInvalidator.register(new ValuationTimeCacheEntry.ValidAtCalculationInstant(valuationTime));
    return valuationTime;
  }

  @Override
  public MarketDataSource getMarketDataSource() {
    return new MarketDataSource() {
      @Override
      public MarketDataItem<?> get(ExternalIdBundle idBundle, FieldName fieldName) {
        _cacheInvalidator.register(idBundle);
        return _delegate.getMarketDataSource().get(idBundle, fieldName);
      }
    };
  }

  @Override
  public Environment withValuationTime(ZonedDateTime valuationTime) {
    return _delegate.withValuationTime(valuationTime);
  }

  @Override
  public Environment withMarketData(MarketDataSource marketData) {
    return _delegate.withMarketData(marketData);
  }

  @Override
  public Environment with(ZonedDateTime valuationTime, MarketDataSource marketData) {
    return new SimpleEnvironment(valuationTime, marketData);
  }
}
