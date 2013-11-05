/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;

/**
 *
 */
/* package */ class ProxyCacheLoader implements CacheLoader {

  private static final Logger s_logger = LoggerFactory.getLogger(ProxyCacheLoader.class);

  /* package */ static final ProxyCacheLoader INSTANCE = new ProxyCacheLoader();

  private ProxyCacheLoader() {
  }

  @Override
  public Object load(Object key) throws CacheException {
    throw new UnsupportedOperationException("load not supported");
  }

  @Override
  public Map loadAll(Collection keys) {
    throw new UnsupportedOperationException("loadAll not supported");
  }

  @Override
  public Object load(Object key, Object receiver) {
    CacheKey cacheKey = (CacheKey) key;
    try {
      // TODO do I need a wrapper object that can rethrow an exception when it's dereferenced?
      return cacheKey.getMethod().invoke(receiver, cacheKey.getArgs());
    } catch (IllegalAccessException | InvocationTargetException e) {
      // TODO handle this better
      s_logger.warn("Failed to populate cache", e);
      return null;
    }
  }

  @Override
  public Map loadAll(Collection keys, Object argument) {
    throw new UnsupportedOperationException("loadAll not supported");
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  @Override
  public void init() {
  }

  @Override
  public void dispose() throws CacheException {
  }

  @Override
  public Status getStatus() {
    return Status.STATUS_ALIVE;
  }
}
