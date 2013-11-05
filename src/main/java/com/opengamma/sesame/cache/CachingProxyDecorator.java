/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.Method;

import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.proxy.ProxyNodeDecorator;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

/**
 * Decorates a node in the graph with a proxy which performs memoization using a cache.
 */
/* package */ class CachingProxyDecorator extends ProxyNodeDecorator {

  private static final net.sf.ehcache.Cache s_cache;

  static {
    // TODO this is just a very basic proof of concept
    CacheConfiguration config = new CacheConfiguration("EngineProxyCache", 1000000)
        .eternal(true)
        .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    s_cache = new net.sf.ehcache.Cache(config);
    CacheManager.getInstance().addCache(s_cache);
  }

  @Override
  protected boolean decorate(InterfaceNode node) {
    Class<?> interfaceType = node.getInterfaceType();
    for (Method method : interfaceType.getMethods()) {
      if (method.getAnnotation(Cache.class) != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Exception {
    // check the method for the annotation, it's possible for the same class to have cached and non-cached methods
    if (method.getAnnotation(Cache.class) == null) {
      return method.invoke(delegate, args);
    } else {
      CacheKey key = new CacheKey(delegate.getClass(), method, args);
      Element element = s_cache.getWithLoader(key, ProxyCacheLoader.INSTANCE, delegate);
      // TODO is the null check necessary?
      if (element == null) {
        return null;
      } else {
        return element.getObjectValue();
      }
    }
  }

  /** Package scoped for testing */
  /* package */ static net.sf.ehcache.Cache getCache() {
    return s_cache;
  }
}
