/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache.source;

import java.util.Collection;
import java.util.Map;

import com.opengamma.core.SourceWithExternalBundle;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.id.ExternalBundleIdentifiable;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.UniqueIdentifiable;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
/* package */ abstract class CacheAwareSourceWithExternalBundle<V extends UniqueIdentifiable & ExternalBundleIdentifiable>
    implements SourceWithExternalBundle<V> {

  private final SourceWithExternalBundle<V> _delegate;
  private final CacheInvalidator _cacheInvalidator;

  /* package */ CacheAwareSourceWithExternalBundle(SourceWithExternalBundle<V> delegate, CacheInvalidator cacheInvalidator) {
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public Collection<V> get(ExternalIdBundle bundle, VersionCorrection versionCorrection) {
    return register(_delegate.get(bundle, versionCorrection));
  }

  @Override
  public Map<ExternalIdBundle, Collection<V>> getAll(Collection<ExternalIdBundle> bundles, VersionCorrection versionCorrection) {
    Map<ExternalIdBundle, Collection<V>> results = _delegate.getAll(bundles, versionCorrection);
    for (Collection<V> items : results.values()) {
      register(items);
    }
    return results;
  }

  @Override
  public Collection<V> get(ExternalIdBundle bundle) {
    return register(_delegate.get(bundle));
  }

  @Override
  public V getSingle(ExternalIdBundle bundle) {
    return register(_delegate.getSingle(bundle));
  }

  @Override
  public V getSingle(ExternalIdBundle bundle, VersionCorrection versionCorrection) {
    return register(_delegate.getSingle(bundle, versionCorrection));
  }

  @Override
  public Map<ExternalIdBundle, V> getSingle(Collection<ExternalIdBundle> bundles, VersionCorrection versionCorrection) {
    return register(_delegate.getSingle(bundles, versionCorrection));
  }

  @Override
  public V get(UniqueId uniqueId) {
    return register(_delegate.get(uniqueId));
  }

  @Override
  public V get(ObjectId objectId, VersionCorrection versionCorrection) {
    return register(_delegate.get(objectId, versionCorrection));
  }

  @Override
  public Map<UniqueId, V> get(Collection<UniqueId> uniqueIds) {
    return register(_delegate.get(uniqueIds));
  }

  @Override
  public Map<ObjectId, V> get(Collection<ObjectId> objectIds, VersionCorrection versionCorrection) {
    return register(_delegate.get(objectIds, versionCorrection));
  }

  @Override
  public ChangeManager changeManager() {
    return _delegate.changeManager();
  }

  private <K> Map<K, V> register(Map<K, V> items) {
    register(items.values());
    return items;
  }

  private Collection<V> register(Collection<V> items) {
    for (UniqueIdentifiable item : items) {
      _cacheInvalidator.register(item.getUniqueId().getObjectId());
    }
    return items;
  }

  /* package */ <T extends V> T register(T item) {
    if (item != null) {
      _cacheInvalidator.register(item.getUniqueId().getObjectId());
    }
    return item;
  }
}
