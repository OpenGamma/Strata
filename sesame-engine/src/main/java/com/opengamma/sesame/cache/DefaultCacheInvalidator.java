/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.inject.Provider;

import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

import net.sf.ehcache.Ehcache;

/**
 * TODO if this turns out to be a point of contention will need to remove the locking and make thread safe
 * or have multiple thread local copies and merge them at the end of the cycle before the invalidation step
 */
public class DefaultCacheInvalidator implements CacheInvalidator {

  // TODO need to know the config version correction during invalidation.
  // if it changes invalidate all cache values that depend on DB data

  private final Provider<Collection<MethodInvocationKey>> _executingMethods;

  private final SetMultimap<ObjectId, MethodInvocationKey> _objectIdsToKeys = HashMultimap.create();

  // TODO should the keys be MarketDataRequirements?
  private final SetMultimap<ExternalId, MethodInvocationKey> _externalIdsToKeys = HashMultimap.create();

  private final List<Pair<MethodInvocationKey, ValuationTimeCacheEntry>> _valuationTimeEntries = Lists.newArrayList();

  private final Ehcache _cache;

  private MarketDataSource _marketDataSource;
  private VersionCorrection _configVersionCorrection;

  public DefaultCacheInvalidator(Provider<Collection<MethodInvocationKey>> executingMethods, Ehcache cache) {
    _cache = ArgumentChecker.notNull(cache, "cache");
    _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
  }

  @Override
  public synchronized void register(ExternalId id) {
    _externalIdsToKeys.putAll(id, _executingMethods.get());
  }

  @Override
  public synchronized void register(ExternalIdBundle bundle) {
    for (ExternalId id : bundle.getExternalIds()) {
      register(id);
    }
  }

  @Override
  public synchronized void register(ObjectId id) {
    if (VersionCorrection.LATEST.equals(_configVersionCorrection)) {
      _objectIdsToKeys.putAll(id, _executingMethods.get());
    }
  }

  @Override
  public synchronized void register(ValuationTimeCacheEntry entry) {
    for (MethodInvocationKey key : _executingMethods.get()) {
      _valuationTimeEntries.add(Pairs.of(key, entry));
    }
  }

  @Override
  public synchronized void invalidate(MarketDataSource marketDataSource,
                                      ZonedDateTime valuationTime,
                                      VersionCorrection configVersionCorrection,
                                      // TODO should this be Collection<MarketDataRequirement>?
                                      Collection<ExternalId> marketData,
                                      Collection<ObjectId> dbIds) {
    ArgumentChecker.notNull(marketDataSource, "marketDataSource");
    ArgumentChecker.notNull(valuationTime, "valuationTime");
    ArgumentChecker.notNull(configVersionCorrection, "configVersionCorrection");
    ArgumentChecker.notNull(marketData, "marketData");
    ArgumentChecker.notNull(dbIds, "dbIds");

    // if the market data provider has changed every value that uses market data is potentially invalid
    if (!marketDataSource.equals(_marketDataSource)) {
      _marketDataSource = marketDataSource;
      _cache.removeAll(_externalIdsToKeys.values());
      _objectIdsToKeys.clear();
    }

    invalidateValuationTime(valuationTime);

    // TODO if the new VC isn't the same as the old then clear all DB dependent entries
    _configVersionCorrection = configVersionCorrection;

    for (ExternalId externalId : marketData) {
      _cache.removeAll(_externalIdsToKeys.removeAll(externalId));
    }
    for (ObjectId objectId : dbIds) {
      _cache.removeAll(_objectIdsToKeys.removeAll(objectId));
    }
  }

  private void invalidateValuationTime(ZonedDateTime valuationTime) {
    for (Iterator<Pair<MethodInvocationKey, ValuationTimeCacheEntry>> itr = _valuationTimeEntries.iterator(); itr.hasNext(); ) {
      Pair<MethodInvocationKey, ValuationTimeCacheEntry> pair = itr.next();
      MethodInvocationKey key = pair.getFirst();
      ValuationTimeCacheEntry timeEntry = pair.getSecond();
      if (!timeEntry.isValidAt(valuationTime)) {
        _cache.remove(key);
        itr.remove();
      }
    }
  }
}
