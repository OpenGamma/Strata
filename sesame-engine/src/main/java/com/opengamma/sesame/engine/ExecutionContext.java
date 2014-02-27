/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collection;
import java.util.Set;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.ValuationTimeCacheEntry;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataSeries;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 *
 */
/* package */ class ExecutionContext {

  // TODO how should this be initialized? view needs to set it at the start of every task.
  // like initializeCycle but on every thread. there must be a neater way
  // make THREAD_CONTEXT mutable? create a new ThreadLocal every cycle with an initialValue() method
  // TODO does this need to be a stack?
  private static final ThreadLocal<ExecutionContext> THREAD_CONTEXT = new ThreadLocal<>();

  // TODO SourceListener - needs to be converted to an interface
  // TODO cache - maybe a cut down version of the Ehcache interface with putIfAbsent, remove, removeAll, get
  // TODO change these to be values instead of objects? i.e. ZonedDateTime, VersionCorrection, MarketDataFactory/Spec?
  // or have a separate context key class?
  // TODO portfolio and config version corrections
  private final ValuationTimeFn _valuationTimeFn;
  private final CacheInvalidator _cacheInvalidator;
  private final MarketDataFn _marketDataFn;

  /* package */ ExecutionContext(ValuationTimeFn valuationTimeFn,
                                 CacheInvalidator cacheInvalidator,
                                 MarketDataFn marketDataFn) {
    _valuationTimeFn = valuationTimeFn;
    _cacheInvalidator = cacheInvalidator;
    _marketDataFn = marketDataFn;
  }

  private static ExecutionContext getContext() {
    return THREAD_CONTEXT.get();
  }

  /* package */ static class ValuationTime implements ValuationTimeFn {

    @Override
    public LocalDate getDate() {
      return getContext()._valuationTimeFn.getDate();
    }

    @Override
    public ZonedDateTime getTime() {
      return getContext()._valuationTimeFn.getTime();
    }
  }

  /* package */ static class Invalidator implements CacheInvalidator {

    @Override
    public void register(ExternalId id) {
      getContext()._cacheInvalidator.register(id);
    }

    @Override
    public void register(ExternalIdBundle bundle) {
      getContext()._cacheInvalidator.register(bundle);
    }

    @Override
    public void register(ObjectId id) {
      getContext()._cacheInvalidator.register(id);
    }

    @Override
    public void register(ValuationTimeCacheEntry entry) {
      getContext()._cacheInvalidator.register(entry);
    }

    @Override
    public void invalidate(MarketDataFactory marketDataFactory,
                           ZonedDateTime valuationTime,
                           VersionCorrection configVersionCorrection,
                           Collection<ExternalId> marketData,
                           Collection<ObjectId> dbData) {
      getContext()._cacheInvalidator.invalidate(marketDataFactory,
                                                valuationTime,
                                                configVersionCorrection,
                                                marketData,
                                                dbData);
    }
  }
  
  /* package */ static class MarketData implements MarketDataFn {

    @Override
    public Result<MarketDataValues> requestData(MarketDataRequirement requirement) {
      return getContext()._marketDataFn.requestData(requirement);
    }

    @Override
    public Result<MarketDataValues> requestData(Set<MarketDataRequirement> requirements) {
      return getContext()._marketDataFn.requestData(requirements);
    }

    @Override
    public Result<MarketDataSeries> requestData(MarketDataRequirement requirement, LocalDateRange dateRange) {
      return getContext()._marketDataFn.requestData(requirement, dateRange);
    }

    @Override
    public Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements,
                                                        LocalDateRange dateRange) {
      return getContext()._marketDataFn.requestData(requirements, dateRange);
    }

    @Override
    public Result<MarketDataSeries> requestData(MarketDataRequirement requirement, Period seriesPeriod) {
      return getContext()._marketDataFn.requestData(requirement, seriesPeriod);
    }

    @Override
    public Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, Period seriesPeriod) {
      return getContext()._marketDataFn.requestData(requirements, seriesPeriod);
    }

  }
}
