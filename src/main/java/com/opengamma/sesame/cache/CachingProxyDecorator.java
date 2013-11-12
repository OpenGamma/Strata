/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.proxy.ProxyNodeDecorator;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

/**
 * Decorates a node in the graph with a proxy which performs memoization using a cache.
 */
public class CachingProxyDecorator extends ProxyNodeDecorator {

  public static final CachingProxyDecorator INSTANCE = new CachingProxyDecorator();

  private static final SelfPopulatingCache s_cache;
  //private static final net.sf.ehcache.Cache s_cache;

  static {
    // TODO this is just a very basic proof of concept
    CacheConfiguration config = new CacheConfiguration("EngineProxyCache", 1000)
        .eternal(true)
        .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    //s_cache = new net.sf.ehcache.Cache(config);
    s_cache = new SelfPopulatingCache(new net.sf.ehcache.Cache(config), new EntryFactory());
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
      CacheKey key = new CacheKey(delegate.getClass(), method, args, delegate);
      // TODO confirm this blocks if multiple threads try to get the same value.
      // looking at the Ehcache source I'm not sure it does. need to lock at the level of the key
      //Element element = s_cache.getWithLoader(key, ProxyCacheLoader.INSTANCE, delegate);
      Element element = s_cache.get(key);
      return element.getObjectValue();
    }
  }

  /** Package scoped for testing */
  /* package */ static /*net.sf.ehcache.Cache*/SelfPopulatingCache getCache() {
    return s_cache;
  }

  private static class EntryFactory implements CacheEntryFactory {

    private static final Logger s_logger = LoggerFactory.getLogger(EntryFactory.class);

    @Override
    public Object createEntry(Object key) throws Exception {
      CacheKey cacheKey = (CacheKey) key;
      try {
        s_logger.debug("Loading value for key {}", cacheKey);
        // TODO do I need a wrapper object that can rethrow an exception when it's dereferenced?
        return cacheKey.getMethod().invoke(cacheKey.getReceiver(), cacheKey.getArgs());
      } catch (IllegalAccessException | InvocationTargetException e) {
        // TODO handle this better
        s_logger.warn("Failed to populate cache", e);
        return null;
      }

    }
  }
}
