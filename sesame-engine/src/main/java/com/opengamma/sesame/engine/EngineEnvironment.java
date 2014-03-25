/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Map;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.ValuationTimeCacheEntry;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * {@link Environment} implementation created and managed by the engine.
 * This allows the engine to monitor what data from the environment is used by each function and invalidate cache
 * entries when the data changes.
 * This class intentionally uses reference equality to avoid unintentional sharing of cached values. This might be
 * changed when we tackle incrementally clearing the cache in live ticking views.
 * This class is package-private because it is only intended to be used by the engine itself, not user code.
 */
/* package */ final class EngineEnvironment implements Environment {

  private final SimpleEnvironment _delegate;
  private final CacheInvalidator _cacheInvalidator;

  /* package */ EngineEnvironment(ZonedDateTime valuationTime,
                                  MarketDataSource marketDataSource,
                                  Map<Class<?>, Object> scenarioArguments,
                                  CacheInvalidator cacheInvalidator) {
    _delegate = new SimpleEnvironment(ArgumentChecker.notNull(valuationTime, "valuationTime"),
                                      ArgumentChecker.notNull(marketDataSource, "marketDataSource"),
                                      ArgumentChecker.notNull(scenarioArguments, "scenarioArguments"));
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
      public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
        _cacheInvalidator.register(id);
        return _delegate.getMarketDataSource().get(id, fieldName);
      }
    };
  }

  @Override
  public Object getScenarioArgument(Class<?> functionType) {
    return _delegate.getScenarioArgument(functionType);
  }

  @Override
  public Environment withValuationTime(ZonedDateTime valuationTime) {
    // this the returned environment is deliberately not one that's managed by the engine
    // TODO link to a thorough explanation of the caching implementation that explains this in detail
    return _delegate.withValuationTime(valuationTime);
  }

  @Override
  public Environment withMarketData(MarketDataSource marketData) {
    // this the returned environment is deliberately not one that's managed by the engine
    // TODO link to a thorough explanation of the caching implementation that explains this in detail
    return _delegate.withMarketData(marketData);
  }

  @Override
  public Environment with(ZonedDateTime valuationTime, MarketDataSource marketData) {
    // this the returned environment is deliberately not one that's managed by the engine
    // TODO link to a thorough explanation of the caching implementation that explains this in detail
    return _delegate.with(valuationTime, marketData);
  }
}
