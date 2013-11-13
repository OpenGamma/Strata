/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.graph.Node;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.proxy.InvocationHandlerFactory;
import com.opengamma.sesame.proxy.ProxyNode;
import com.opengamma.util.ArgumentChecker;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

/**
 * Decorates a node in the graph with a proxy which performs memoization using a cache.
 */
public class CachingProxyDecorator implements NodeDecorator {

  public static final CachingProxyDecorator INSTANCE = new CachingProxyDecorator();

  // TODO make this an instance field
  private static final SelfPopulatingCache s_cache;

  static {
    // TODO this is just a very basic proof of concept
    CacheConfiguration config = new CacheConfiguration("EngineProxyCache", 1000)
        .eternal(true)
        .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    s_cache = new SelfPopulatingCache(new net.sf.ehcache.Cache(config), new EntryFactory());
    CacheManager.getInstance().addCache(s_cache);
  }

  private CachingProxyDecorator() {
  }

  @Override
  public Node decorateNode(Node node) {
    if (!(node instanceof ProxyNode) && !(node instanceof InterfaceNode)) {
      return node;
    }
    Class<?> interfaceType;
    Class<?> implementationType;
    if (node instanceof InterfaceNode) {
      implementationType = ((InterfaceNode) node).getType();
      interfaceType = ((InterfaceNode) node).getInterfaceType();
    } else {
      implementationType = ((ProxyNode) node).getImplementationType();
      interfaceType = ((ProxyNode) node).getInterfaceType();
    }
    for (Method method : interfaceType.getMethods()) {
      if (method.getAnnotation(Cache.class) != null) {
        return new ProxyNode(node, interfaceType, implementationType, new HandlerFactory(implementationType, interfaceType));
      }
    }
    for (Method method : implementationType.getMethods()) {
      if (method.getAnnotation(Cache.class) != null) {
        return new ProxyNode(node, interfaceType, implementationType, new HandlerFactory(implementationType, interfaceType));
      }
    }
    return node;
  }

  /**
   * Creates an instance of {@link Handler} when the graph is built.
   */
  private static class HandlerFactory implements InvocationHandlerFactory {

    private final Class<?> _interfaceType;
    private final Class<?> _implementationType;

    private HandlerFactory(Class<?> implementationType, Class<?> interfaceType) {
      _implementationType = ArgumentChecker.notNull(implementationType, "implementationType");
      _interfaceType = ArgumentChecker.notNull(interfaceType, "interfaceType");
    }

    @Override
    public InvocationHandler create(Object delegate, ProxyNode node) {
      Set<Method> cachedMethods = Sets.newHashSet();
      for (Method method : _interfaceType.getMethods()) {
        if (method.getAnnotation(Cache.class) != null) {
          cachedMethods.add(method);
        }
      }
      for (Method method : _implementationType.getMethods()) {
        if (method.getAnnotation(Cache.class) != null) {
          // the proxy will always see the interface method. no point caching the instance method
          // need to go up the inheritance hierarchy and find all interface methods implemented by this method
          // and cache those
          for (Class<?> iface : ConfigUtils.getInterfaces(_implementationType)) {
            try {
              Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
              cachedMethods.add(ifaceMethod);
            } catch (NoSuchMethodException e) {
              // expected
            }
          }
        }
      }
      return new Handler(delegate, cachedMethods, _implementationType);
    }
  }

  /**
   * Handles method invocations and possibly returns a cached result instead of calling the underlying object.
   * If the method doesn't have a {@link Cache} annotation the underlying object is called.
   * If the cache contains an element that corresponds to the method and arguments it's returned and the underlying
   * object isn't called.
   * If the cache doesn't contain an element the underlying object is called and
   * the result is cached (via {@link EntryFactory}).
   */
  private static class Handler implements InvocationHandler {

    private final Object _delegate;
    private final Set<Method> _cachedMethods;
    private final Class<?> _implementationType;

    private Handler(Object delegate, Set<Method> cachedMethods, Class<?> implementationType) {
      _delegate = ArgumentChecker.notNull(delegate, "delegate");
      _cachedMethods = ArgumentChecker.notNull(cachedMethods, "cachedMethods");
      _implementationType = ArgumentChecker.notNull(implementationType, "implementationType");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (_cachedMethods.contains(method)) {
        MethodInvocationKey key = new MethodInvocationKey(_implementationType, method, args, _delegate);
        Element element = s_cache.get(key);
        return element.getObjectValue();
      } else {
        try {
          return method.invoke(_delegate, args);
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }
    }
  }

  /* package */ static SelfPopulatingCache getCache() {
    return s_cache;
  }

  /**
   * Populates the cache when a lookup is done and no object is found. A lookup is keyed by a method invocation -
   * the Method object and the arguments. If there is no cached value the method is invoked and its return value
   * is cached.
   */
  private static class EntryFactory implements CacheEntryFactory {

    private static final Logger s_logger = LoggerFactory.getLogger(EntryFactory.class);

    @Override
    public Object createEntry(Object key) throws Exception {
      MethodInvocationKey cacheKey = (MethodInvocationKey) key;
      try {
        s_logger.debug("Loading value for key {}", cacheKey);
        return cacheKey.invoke();
      } catch (IllegalAccessException | InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof Error) {
          throw ((Error) cause);
        } else {
          throw ((Exception) cause);
        }
      }
    }
  }
}
