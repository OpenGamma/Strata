/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;

import net.sf.ehcache.Ehcache;

/**
 * TODO if this turns out to be a point of contention will need to remove the locking and make thread safe
 */
/* package */ class CacheInvalidator {

  private final Provider<Collection<MethodInvocationKey>> _executingMethods;
  private final Map<Object, Set<MethodInvocationKey>> _idsToKeys = Maps.newHashMap();
  private final Ehcache _cache;

  /* package */ CacheInvalidator(Provider<Collection<MethodInvocationKey>> executingMethods, Ehcache cache) {
    _cache = ArgumentChecker.notNull(cache, "cache");
    _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
  }

  // provider decorators register IDs when they're requested (e.g. market data, config)
  /* package */ synchronized void register(Object... ids) {
    register(Arrays.asList(ids));
  }

  /* package */ synchronized void register(Collection<Object> ids) {
    for (Object id : ids) {
      if (id instanceof ExternalIdBundle) {
        for (ExternalId externalId : ((ExternalIdBundle) id).getExternalIds()) {
          registerSingle(externalId);
        }
      } else {
        registerSingle(id);
      }
    }
  }

  private void registerSingle(Object id) {
    Set<MethodInvocationKey> methodsForId = _idsToKeys.get(id);
    if (methodsForId == null) {
      methodsForId = Sets.newHashSet(_executingMethods.get());
      _idsToKeys.put(id, methodsForId);
    } else {
      methodsForId.addAll(_executingMethods.get());
    }
  }

  /* package */ synchronized void invalidate(Object... ids) {
    invalidate(Arrays.asList(ids));
  }

  // engine calls this between cycles with the ids of everything that's updated
  // corresponding cache keys are looked up and cleared out
  /* package */ synchronized void invalidate(Collection<Object> ids) {
    for (Object id : ids) {
      if (id instanceof ExternalIdBundle) {
        for (ExternalId externalId : ((ExternalIdBundle) id).getExternalIds()) {
          invalidateSingle(externalId);
        }
      } else {
        invalidateSingle(id);
      }
    }
  }

  private void invalidateSingle(Object id) {
    Set<MethodInvocationKey> keys = _idsToKeys.remove(id);
    if (keys != null) {
      _cache.removeAll(keys);
    }
  }
}
