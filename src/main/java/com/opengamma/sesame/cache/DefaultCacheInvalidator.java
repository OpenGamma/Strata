/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

import net.sf.ehcache.Ehcache;

/**
 * TODO if this turns out to be a point of contention will need to remove the locking and make thread safe
 * or have multiple thread local copies and merge them at the end of the cycle before the invalidation step
 */
public class DefaultCacheInvalidator implements CacheInvalidator {

  // TODO pairs of methods - register/invalidateMarketData, register/invalidateConfig, register/invalidateValuationTime?

  private final Provider<Collection<MethodInvocationKey>> _executingMethods;
  // TODO multiple separate maps for market data, config objects?
  // need to clear market data when data provider spec changes
  private final SetMultimap<Object, MethodInvocationKey> _idsToKeys = HashMultimap.create();
  private final List<Pair<MethodInvocationKey, ValuationTimeCacheEntry>> valuationTimeEntries = Lists.newArrayList();
  private final Ehcache _cache;

  public DefaultCacheInvalidator(Provider<Collection<MethodInvocationKey>> executingMethods, Ehcache cache) {
    _cache = ArgumentChecker.notNull(cache, "cache");
    _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
  }

  @Override
  public synchronized void register(ExternalId id) {
    registerSingle(id);
  }

  @Override
  public synchronized void register(ExternalIdBundle bundle) {
    for (ExternalId id : bundle.getExternalIds()) {
      registerSingle(id);
    }
  }

  @Override
  public synchronized void register(ObjectId id) {
    registerSingle(id);
  }

  private void registerSingle(Object id) {
    _idsToKeys.putAll(id, _executingMethods.get());
  }

  @Override
  public synchronized void invalidate(ExternalId id) {
    invalidateSingle(id);
  }

  @Override
  public synchronized void invalidate(ObjectId id) {
    invalidateSingle(id);
  }

  private void invalidateSingle(Object id) {
    Set<MethodInvocationKey> keys = _idsToKeys.removeAll(id);
    if (keys != null) {
      _cache.removeAll(keys);
    }
  }

  @Override
  public synchronized void register(ValuationTimeCacheEntry entry) {
    for (MethodInvocationKey key : _executingMethods.get()) {
      valuationTimeEntries.add(Pairs.of(key, entry));
    }
  }

  @Override
  public synchronized void invalidate(ZonedDateTime valuationTime) {
    for (Iterator<Pair<MethodInvocationKey, ValuationTimeCacheEntry>> itr = valuationTimeEntries.iterator(); itr.hasNext(); ) {
      Pair<MethodInvocationKey, ValuationTimeCacheEntry> pair = itr.next();
      MethodInvocationKey key = pair.getFirst();
      ValuationTimeCacheEntry timeEntry = pair.getSecond();
      if (!timeEntry.isValidAt(valuationTime)) {
        _cache.remove(key);
        itr.remove();
      }
    }
  }

  @Override
  public synchronized void setDataSource(MarketDataFactory marketDataFactory) {
    // TODO implement
    // if marketDataFactory equals current marketDataFactory do nothing except update marketDataFactory
    // otherwise clear all MD dependent values and update marketDataFactory
    // probably best to separate MD values from DB values instead of putting them all in _idsToKeys
  }
}
