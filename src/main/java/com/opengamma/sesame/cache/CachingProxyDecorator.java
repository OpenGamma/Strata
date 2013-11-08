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
public class CachingProxyDecorator extends ProxyNodeDecorator {

  public static final CachingProxyDecorator INSTANCE = new CachingProxyDecorator();

  private static final net.sf.ehcache.Cache s_cache;

  static {
    // TODO this is just a very basic proof of concept
    CacheConfiguration config = new CacheConfiguration("EngineProxyCache", 1000000)
        .eternal(true)
        .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    s_cache = new net.sf.ehcache.Cache(config);
    CacheManager.getInstance().addCache(s_cache);
  }

  private CachingProxyDecorator() {
  }

  @Override
  protected boolean decorate(InterfaceNode node) {
    // TODO should this look on the interface or implementation methods? or both? probably both
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
    // TODO should be able to put the @Cache annotation on the impl
    // don't know the impl class here so can't check the method
    // create a proxy instance per proxied node and store the delegate type?
    // can't check the delegate, it might be another proxy, not the impl instance
    // if the concrete type is passed in will it be a lot worse than knowing it up front?
    // could I do some instance level caching of the methods if I have one proxy instance per node?
    if (method.getAnnotation(Cache.class) == null) {
      return method.invoke(delegate, args);
    } else {
      CacheKey key = new CacheKey(delegate.getClass(), method, args);
      // TODO confirm this blocks if multiple threads try to get the same value
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
