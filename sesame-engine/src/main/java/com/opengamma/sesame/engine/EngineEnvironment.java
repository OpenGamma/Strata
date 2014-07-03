/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Map;
import java.util.Objects;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.ValuationTimeCacheEntry;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * {@link Environment} implementation created and managed by the engine.
 * This allows the engine to monitor what data from the environment is used
 * by each function and invalidate cache entries when the data changes. This
 * class is package-private because it is only intended to be used by the
 * engine itself, not user code.
 */
final class EngineEnvironment implements Environment {

  /** The valuation time. */
  private final ZonedDateTime _valuationTime;

  private final CycleMarketDataFactory _cycleMarketDataFactory;

  /** The source of market data. */
  private final MarketDataSource _marketDataSource;

  /** Scenario arguments, keyed by the type of function implementation that uses them. */
  private final ImmutableMap<Class<?>, Object> _scenarioArguments;

  private final CacheInvalidator _cacheInvalidator;

  EngineEnvironment(ZonedDateTime valuationTime,
                                  CycleMarketDataFactory cycleMarketDataFactory,
                                  Map<Class<?>, Object> scenarioArguments,
                                  CacheInvalidator cacheInvalidator) {
    this(valuationTime, cycleMarketDataFactory, cycleMarketDataFactory.getPrimaryMarketDataSource(),
         scenarioArguments, cacheInvalidator);
  }

  private EngineEnvironment(ZonedDateTime valuationTime,
                            CycleMarketDataFactory cycleMarketDataFactory,
                            MarketDataSource marketDataSource,
                            Map<Class<?>, Object> scenarioArguments,
                            CacheInvalidator cacheInvalidator) {

    _valuationTime = ArgumentChecker.notNull(valuationTime, "valuationTime");
    _cycleMarketDataFactory = ArgumentChecker.notNull(cycleMarketDataFactory, "cycleMarketDataFactory");
    _scenarioArguments = ImmutableMap.copyOf(ArgumentChecker.notNull(scenarioArguments, "scenarioArguments"));
    _marketDataSource = marketDataSource;
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
  }

  @Override
  public LocalDate getValuationDate() {
    LocalDate valuationDate = _valuationTime.toLocalDate();
    _cacheInvalidator.register(new ValuationTimeCacheEntry.ValidOnCalculationDay(valuationDate));
    return valuationDate;
  }

  @Override
  public ZonedDateTime getValuationTime() {
    _cacheInvalidator.register(new ValuationTimeCacheEntry.ValidAtCalculationInstant(_valuationTime));
    return _valuationTime;
  }

  @Override
  public MarketDataSource getMarketDataSource() {
    return new MarketDataSource() {
      @Override
      public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
        _cacheInvalidator.register(id);
        return _marketDataSource.get(id, fieldName);
      }
    };
  }

  @Override
  public Object getScenarioArgument(Object function) {
    return _scenarioArguments.get(ArgumentChecker.notNull(function, "function").getClass());
  }

  @Override
  public Map<Class<?>, Object> getScenarioArguments() {
    return _scenarioArguments;
  }

  @Override
  public Environment withValuationTime(ZonedDateTime valuationTime) {
    MarketDataSource marketDataSource = _cycleMarketDataFactory.getMarketDataSourceForDate(valuationTime);
    return new EngineEnvironment(valuationTime, _cycleMarketDataFactory, marketDataSource,
                                 _scenarioArguments, _cacheInvalidator);
  }

  @Override
  public Environment withValuationTimeAndFixedMarketData(ZonedDateTime valuationTime) {
    return new EngineEnvironment(valuationTime, _cycleMarketDataFactory, _marketDataSource,
                                 _scenarioArguments, _cacheInvalidator);
  }

  @Override
  public Environment withMarketData(MarketDataSource marketDataSource) {
    return new EngineEnvironment(_valuationTime, _cycleMarketDataFactory, marketDataSource,
                                 _scenarioArguments, _cacheInvalidator);
  }

  @Override
  public Environment withScenarioArguments(Map<Class<?>, Object> scenarioArguments) {
    return new EngineEnvironment(_valuationTime, _cycleMarketDataFactory, _marketDataSource,
                                 scenarioArguments, _cacheInvalidator);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    EngineEnvironment that = (EngineEnvironment) o;
    return _valuationTime.equals(that._valuationTime) &&
        _marketDataSource.equals(that._marketDataSource) &&
        _scenarioArguments.equals(that._scenarioArguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_valuationTime, _marketDataSource, _scenarioArguments);
  }
}
