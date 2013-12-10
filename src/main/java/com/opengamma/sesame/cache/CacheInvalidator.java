/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.util.Collection;
import java.util.Set;

import javax.inject.Provider;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.util.ArgumentChecker;

import net.sf.ehcache.Ehcache;

/**
 * TODO if this turns out to be a point of contention will need to remove the locking and make thread safe
 * or have multiple thread local copies and merge them at the end of the cycle before the invalidation step
 */
/* package */ class CacheInvalidator {

  // TODO valuation time. method to register and invalidate
  // need to store the time the value was calculated
  // when invalidating by valuation time can check the lifetime and the calculation time and possibly retain the value
  // is lifetime implied by the method called on ValuationTimeFn? getDate() implies 1 day lifetime
  // getTime() implies it's only valid for the instant it's calculated

  // TODO pairs of methods - register/invalidateMarketData, register/invalidateConfig, register/invalidateValuationTime?

  private final Provider<Collection<MethodInvocationKey>> _executingMethods;
  // TODO multiple separate maps for market data, config objects?
  // need to clear market data when data provider spec changes
  private final SetMultimap<Object, MethodInvocationKey> _idsToKeys = HashMultimap.create();
  private final Ehcache _cache;

  /* package */ CacheInvalidator(Provider<Collection<MethodInvocationKey>> executingMethods, Ehcache cache) {
    _cache = ArgumentChecker.notNull(cache, "cache");
    _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
  }

  /* package */ synchronized void register(ExternalId id) {
    registerSingle(id);
  }

  /* package */ synchronized void register(ExternalIdBundle bundle) {
    for (ExternalId id : bundle.getExternalIds()) {
      registerSingle(id);
    }
  }

  /* package */ synchronized void register(ObjectId id) {
    registerSingle(id);
  }

  private void registerSingle(Object id) {
    _idsToKeys.putAll(id, _executingMethods.get());
  }

  /* package */ synchronized void invalidate(ExternalId id) {
    invalidateSingle(id);
  }

  /* package */ synchronized void invalidate(ObjectId id) {
    invalidateSingle(id);
  }

  private void invalidateSingle(Object id) {
    Set<MethodInvocationKey> keys = _idsToKeys.removeAll(id);
    if (keys != null) {
      _cache.removeAll(keys);
    }
  }
}
