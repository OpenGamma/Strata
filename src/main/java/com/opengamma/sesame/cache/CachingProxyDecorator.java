/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

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
import com.opengamma.util.ehcache.EHCacheUtils;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * Decorates a node in the graph with a proxy which performs memoization using a cache.
 * TODO thorough docs for the basis of caching, i.e. has to be the same function instance but instances are shared
 */
public class CachingProxyDecorator implements NodeDecorator, AutoCloseable {

  private static final Logger s_logger = LoggerFactory.getLogger(CachingProxyDecorator.class);

  private static final String VIEW_CACHE = "ViewCache";
  private static final AtomicLong s_nextCacheId = new AtomicLong(0);

  private final Ehcache _cache;
  private final ExecutingMethodsThreadLocal _executingMethods;
  private final CacheManager _cacheManager;
  private final String _cacheName;

  public CachingProxyDecorator(CacheManager cacheManager, ExecutingMethodsThreadLocal executingMethods) {
    _cacheManager = ArgumentChecker.notNull(cacheManager, "cacheManager");
    _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
    _cacheName = VIEW_CACHE + s_nextCacheId.getAndIncrement();
    EHCacheUtils.addCache(cacheManager, _cacheName);
    _cache = EHCacheUtils.getCacheFromManager(cacheManager, _cacheName);
  }

  @Override
  public Node decorateNode(Node node) {
    if (!(node instanceof ProxyNode) && !(node instanceof InterfaceNode)) {
      return node;
    }
    Class<?> interfaceType;
    Class<?> implementationType;
    if (node instanceof InterfaceNode) {
      implementationType = ((InterfaceNode) node).getImplementationType();
      interfaceType = ((InterfaceNode) node).getType();
    } else {
      implementationType = ((ProxyNode) node).getImplementationType();
      interfaceType = ((ProxyNode) node).getType();
    }
    if (ConfigUtils.hasMethodAnnotation(interfaceType, Cache.class) ||
        ConfigUtils.hasMethodAnnotation(implementationType, Cache.class)) {
      CachingHandlerFactory handlerFactory = new CachingHandlerFactory(implementationType, interfaceType, _cache, _executingMethods);
      return new ProxyNode(node, interfaceType, implementationType, handlerFactory);
    }
    return node;
  }

  public Ehcache getCache() {
    return _cache;
  }

  @Override
  public void close() throws Exception {
    _cacheManager.removeCache(_cacheName);
  }

  /**
   * Creates an instance of {@link Handler} when the graph is built.
   */
  private static final class CachingHandlerFactory implements InvocationHandlerFactory {

    private final Class<?> _interfaceType;
    private final Class<?> _implementationType;
    private final Ehcache _cache;
    private final ExecutingMethodsThreadLocal _executingMethods;

    private CachingHandlerFactory(Class<?> implementationType,
                                  Class<?> interfaceType,
                                  Ehcache cache,
                                  ExecutingMethodsThreadLocal executingMethods) {
      _executingMethods = executingMethods;
      _cache = ArgumentChecker.notNull(cache, "cache");
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
      // TODO delegate could be a proxy but the cache key should contain the underlying shared function instance
      // probably need 1) an extra argument and 2) a way of drilling through proxies to get to the real receiver
      return new Handler(delegate, cachedMethods, _cache, _executingMethods);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_interfaceType, _implementationType);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final CachingHandlerFactory other = (CachingHandlerFactory) obj;
      return
          Objects.equals(this._interfaceType, other._interfaceType) &&
          Objects.equals(this._implementationType, other._implementationType);
    }
  }

  /**
   * Handles method invocations and possibly returns a cached result instead of calling the underlying object.
   * If the method doesn't have a {@link Cache} annotation the underlying object is called.
   * If the cache contains an element that corresponds to the method and arguments it's returned and the underlying
   * object isn't called.
   * If the cache doesn't contain an element the underlying object is called and the cache is populated.
   * The values in the cache are futures. This allows multiple threads to request the same value and for all of
   * them to block while the first thread calculates it.
   * This is package scoped for testing.
   */
  /* package */ static final class Handler implements InvocationHandler {

    // TODO this could be a proxy but also need the real receiver for the cache key
    private final Object _delegate;
    private final Set<Method> _cachedMethods;
    private final Ehcache _cache;
    private final ExecutingMethodsThreadLocal _executingMethods;

    private Handler(Object delegate,
                    Set<Method> cachedMethods,
                    Ehcache cache,
                    ExecutingMethodsThreadLocal executingMethods) {
      _cache = ArgumentChecker.notNull(cache, "cache");
      _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
      _delegate = ArgumentChecker.notNull(delegate, "delegate");
      _cachedMethods = ArgumentChecker.notNull(cachedMethods, "cachedMethods");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
      if (_cachedMethods.contains(method)) {
        final MethodInvocationKey key = new MethodInvocationKey(_delegate, method, args);
        Element element = _cache.get(key);
        if (element != null) {
          FutureTask<Object> task = (FutureTask<Object>) element.getObjectValue();
          s_logger.debug("Returning cached value for key {}", key);
          return task.get();
        }
        FutureTask<Object> task = new FutureTask<>(new CallableMethod(key, method, args));
        Element previous = _cache.putIfAbsent(new Element(key, task));
        // our task is the one in the cache, run it
        if (previous == null) {
          s_logger.debug("Calculating value for key {}", key);
          task.run();
          return task.get();
        } else {
          // someone else's task is there already, block until it completes
          s_logger.debug("Waiting for cached value to be calculated for key {}", key);
          return ((Future<Object>) previous.getObjectValue()).get();
        }
      } else {
        try {
          s_logger.debug("Calculating non-cacheable result by invoking method {}", method);
          return method.invoke(_delegate, args);
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }
    }

    /** Visible for testing */
    /* package */ Object getDelegate() {
      return _delegate;
    }

    private class CallableMethod implements Callable<Object> {

      private final MethodInvocationKey _key;
      private final Method _method;
      private final Object[] _args;

      public CallableMethod(MethodInvocationKey key, Method method, Object[] args) {
        _key = key;
        _method = method;
        _args = args;
      }

      @Override
      public Object call() throws Exception {
        try {
          _executingMethods.push(_key);
          return _method.invoke(_delegate, _args);
        } catch (IllegalAccessException | InvocationTargetException e) {
          Throwable cause = e.getCause();
          if (cause instanceof Error) {
            throw ((Error) cause);
          } else {
            throw ((Exception) cause);
          }
        } finally {
          _executingMethods.pop();
        }
      }
    }
  }
}
