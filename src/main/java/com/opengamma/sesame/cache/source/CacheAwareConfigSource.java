/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache.source;

import java.util.Collection;
import java.util.Map;

import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.UniqueIdentifiable;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class CacheAwareConfigSource implements ConfigSource {

  private final ConfigSource _delegate;
  private final CacheInvalidator _cacheInvalidator;

  /* package */
  public CacheAwareConfigSource(ConfigSource delegate, CacheInvalidator cacheInvalidator) {
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public ConfigItem<?> get(UniqueId uniqueId) {
    return register(_delegate.get(uniqueId));
  }

  @Override
  public ConfigItem<?> get(ObjectId objectId, VersionCorrection versionCorrection) {
    return register(_delegate.get(objectId, versionCorrection));
  }

  @Override
  public <R> Collection<ConfigItem<R>> get(Class<R> clazz, String configName, VersionCorrection versionCorrection) {
    return register(_delegate.get(clazz, configName, versionCorrection));
  }

  @Override
  public <R> Collection<ConfigItem<R>> getAll(Class<R> clazz, VersionCorrection versionCorrection) {
    return register(_delegate.getAll(clazz, versionCorrection));
  }

  @Override
  public <R> R getConfig(Class<R> clazz, UniqueId uniqueId) {
    R config = _delegate.getConfig(clazz, uniqueId);
    // if there's no config with the specified ID the delegate will throw an exception and registration won't happen
    _cacheInvalidator.register(uniqueId.getObjectId());
    return config;
  }

  @Override
  public <R> R getConfig(Class<R> clazz, ObjectId objectId, VersionCorrection versionCorrection) {
    R config = _delegate.getConfig(clazz, objectId, versionCorrection);
    // if there's no config with the specified ID the delegate will throw an exception and registration won't happen
    _cacheInvalidator.register(objectId);
    return config;
  }

  @Override
  public <R> R getSingle(Class<R> clazz, String configName, VersionCorrection versionCorrection) {
    Collection<ConfigItem<R>> result = get(clazz, configName, versionCorrection);
    if (result.isEmpty()) {
      return null;
    } else {
      ConfigItem<R> item = result.iterator().next();
      register(item);
      return item.getValue();
    }
  }

  @Override
  public <R> R getLatestByName(Class<R> clazz, String name) {
    Collection<ConfigItem<R>> result = get(clazz, name, VersionCorrection.LATEST);
    if (result.isEmpty()) {
      return null;
    } else {
      ConfigItem<R> item = result.iterator().next();
      register(item);
      return item.getValue();
    }
  }

  @Override
  public Map<UniqueId, ConfigItem<?>> get(Collection<UniqueId> uniqueIds) {
    return register(_delegate.get(uniqueIds));
  }

  @Override
  public Map<ObjectId, ConfigItem<?>> get(Collection<ObjectId> objectIds, VersionCorrection versionCorrection) {
    return register(_delegate.get(objectIds, versionCorrection));
  }

  @Override
  public ChangeManager changeManager() {
    return _delegate.changeManager();
  }

  private <K> Map<K, ConfigItem<?>> register(Map<K, ConfigItem<?>> items) {
    register(items.values());
    return items;
  }

  private <T extends ConfigItem<?>> Collection<T> register(Collection<T> items) {
    for (UniqueIdentifiable item : items) {
      _cacheInvalidator.register(item.getUniqueId().getObjectId());
    }
    return items;
  }

  private <T extends ConfigItem<?>> T register(T item) {
    if (item != null) {
      _cacheInvalidator.register(item.getUniqueId().getObjectId());
    }
    return item;
  }
}
