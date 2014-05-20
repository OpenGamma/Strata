/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.graph.ClassNode;
import com.opengamma.sesame.graph.FunctionModelNode;
import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.graph.ProxyNode;
import com.opengamma.sesame.proxy.AbstractProxyInvocationHandler;
import com.opengamma.sesame.proxy.InvocationHandlerFactory;
import com.opengamma.sesame.proxy.ProxyInvocationHandler;
import com.opengamma.util.ArgumentChecker;

/**
 * Decorates a node in the graph with a proxy which performs memoization using a cache.
 * TODO thorough docs for the basis of caching, i.e. has to be the same function instance but instances are shared
 */
public class CachingProxyDecorator extends NodeDecorator {

  private static final Logger s_logger = LoggerFactory.getLogger(CachingProxyDecorator.class);

  private final Cache<MethodInvocationKey, FutureTask<Object>> _cache;
  private final ExecutingMethodsThreadLocal _executingMethods;

  /**
   * @param cache the cache used to store the calculated values
   * @param executingMethods records the currently executing methods and allows cache entries to be removed when
   *   the underlying data used to calculate them changes
   */
  public CachingProxyDecorator(Cache<MethodInvocationKey, FutureTask<Object>> cache,
                               ExecutingMethodsThreadLocal executingMethods) {
    _cache = ArgumentChecker.notNull(cache, "cache");
    _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
  }

  @Override
  public FunctionModelNode decorateNode(FunctionModelNode node) {
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
    if (EngineUtils.hasMethodAnnotation(interfaceType, Cacheable.class) ||
        EngineUtils.hasMethodAnnotation(implementationType, Cacheable.class)) {
      Set<Class<?>> subtreeTypes = subtreeImplementationTypes(node);
      CachingHandlerFactory handlerFactory =
          new CachingHandlerFactory(implementationType, interfaceType, _cache, _executingMethods, subtreeTypes);
      return createProxyNode(node, interfaceType, implementationType, handlerFactory);
    }
    return node;
  }

  /**
   * Returns the types built by all nodes in the node's subtree.
   *
   * @param node a node
   * @return the set of all node types in the node's subtree
   */
  private static Set<Class<?>> subtreeImplementationTypes(FunctionModelNode node) {
    Set<Class<?>> types = new HashSet<>();
    populateSubtreeImplementationTypes(node, types);
    return types;
  }

  private static void populateSubtreeImplementationTypes(FunctionModelNode node, Set<Class<?>> accumulator) {
    // we only want the types for real function nodes, not proxies
    FunctionModelNode concreteNode = node.getConcreteNode();

    if (concreteNode instanceof ClassNode) {
      accumulator.add(((ClassNode) concreteNode).getImplementationType());
    }
    for (FunctionModelNode childNode : node.getDependencies()) {
      populateSubtreeImplementationTypes(childNode, accumulator);
    }
  }

  /**
   * Creates an instance of {@link Handler} when the graph is built.
   * The handler is invoked when a cacheable method is called and takes care of returning a cached result
   * or calculating one and putting it in the cache.
   */
  private static final class CachingHandlerFactory implements InvocationHandlerFactory {

    private final Class<?> _interfaceType;
    private final Class<?> _implementationType;
    private final Cache<MethodInvocationKey, FutureTask<Object>> _cache;
    private final ExecutingMethodsThreadLocal _executingMethods;
    private final Set<Class<?>> _subtreeTypes;

    private CachingHandlerFactory(Class<?> implementationType,
                                  Class<?> interfaceType,
                                  Cache<MethodInvocationKey, FutureTask<Object>> cache,
                                  ExecutingMethodsThreadLocal executingMethods,
                                  Set<Class<?>> subtreeTypes) {
      _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
      _subtreeTypes = ArgumentChecker.notNull(subtreeTypes, "subtreeTypes");
      _cache = ArgumentChecker.notNull(cache, "cache");
      _implementationType = ArgumentChecker.notNull(implementationType, "implementationType");
      _interfaceType = ArgumentChecker.notNull(interfaceType, "interfaceType");
    }

    @Override
    public ProxyInvocationHandler create(Object delegate, ProxyNode node) {
      Set<Method> cachedMethods = Sets.newHashSet();
      for (Method method : _interfaceType.getMethods()) {
        if (method.getAnnotation(Cacheable.class) != null) {
          cachedMethods.add(method);
        }
      }
      for (Method method : _implementationType.getMethods()) {
        if (method.getAnnotation(Cacheable.class) != null) {
          // the proxy will always see the interface method. no point caching the instance method
          // need to go up the inheritance hierarchy and find all interface methods implemented by this method
          // and cache those
          for (Class<?> iface : EngineUtils.getInterfaces(_implementationType)) {
            try {
              Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
              cachedMethods.add(ifaceMethod);
            } catch (NoSuchMethodException e) {
              // expected
            }
          }
        }
      }
      return new Handler(delegate, cachedMethods, _cache, _executingMethods, _subtreeTypes);
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
   * If the method doesn't have a {@link Cacheable} annotation the underlying object is called.
   * If the cache contains an element that corresponds to the method and arguments it's returned and the underlying
   * object isn't called.
   * If the cache doesn't contain an element the underlying object is called and the cache is populated.
   * The values in the cache are futures. This allows multiple threads to request the same value and for all of
   * them to block while the first thread calculates it.
   * This is package scoped for testing.
   */
  /* package */ static final class Handler extends AbstractProxyInvocationHandler {

    private final Object _delegate;
    private final Object _proxiedObject;
    private final Set<Method> _cachedMethods;
    private final Cache<MethodInvocationKey, FutureTask<Object>> _cache;
    private final ExecutingMethodsThreadLocal _executingMethods;
    private final Set<Class<?>> _subtreeTypes;

    private Handler(Object delegate,
                    Set<Method> cachedMethods,
                    Cache<MethodInvocationKey, FutureTask<Object>> cache,
                    ExecutingMethodsThreadLocal executingMethods,
                    Set<Class<?>> subtreeTypes) {
      super(delegate);
      _subtreeTypes = ArgumentChecker.notNull(subtreeTypes, "subtreeTypes");
      _cache = ArgumentChecker.notNull(cache, "cache");
      _executingMethods = ArgumentChecker.notNull(executingMethods, "executingMethods");
      _delegate = ArgumentChecker.notNull(delegate, "delegate");
      _cachedMethods = ArgumentChecker.notNull(cachedMethods, "cachedMethods");
      _proxiedObject = EngineUtils.getProxiedObject(delegate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
      if (_cachedMethods.contains(method)) {
        Object[] keyArgs = getArgumentsForCacheKey(args);
        MethodInvocationKey key = new MethodInvocationKey(_proxiedObject, method, keyArgs);
        FutureTask<Object> cachedTask = _cache.getIfPresent(key);
        if (cachedTask != null) {
          s_logger.debug("Returning cached value for key {}", key);
          return cachedTask.get();
        }
        FutureTask<Object> task = new FutureTask<>(new CallableMethod(key, method, args));
        FutureTask<Object> previous = _cache.asMap().putIfAbsent(key, task);
        // our task is the one in the cache, run it
        if (previous == null) {
          s_logger.debug("Calculating value for hash {}, key {}", key.hashCode(), key);
          task.run();
          return task.get();
        } else {
          // someone else's task is there already, block until it completes
          s_logger.debug("Waiting for cached value to be calculated for key {}", key);
          return previous.get();
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

    /**
     * <p>Returns the method call arguments that should be used in the cache key for the call's return value.
     * If the input arguments don't have an {@link Environment} as their first element they are returned.
     * If the input arguments have an environment as their first element a new set of arguments is returned
     * that is a copy of the input arguments but containing a different environment.</p>
     *
     * <p>The new environment is copied from the environment in the input but uses a different set of scenario
     * arguments. The new arguments only include the arguments for the functions below this function
     * in the graph.</p>
     *
     * <p>Scenarios above the current function in the graph can't affect the function's return value. If they
     * were included in the cache key they could cause a cache miss even though there is no way they can
     * invalidate the cache entry. By removing those arguments from the key we ensure that the only arguments
     * in the key are the ones that can change this function's return value.</p>
     *
     * @param args the arguments to a method call
     * @return the arguments that should be used in the cache key
     */
    private Object[] getArgumentsForCacheKey(Object[] args) {
      if (args == null || args.length == 0 || !(args[0] instanceof Environment)) {
        return args;
      }
      Object[] keyArgs;
      keyArgs = new Object[args.length];
      System.arraycopy(args, 0, keyArgs, 0, args.length);
      Environment env = (Environment) args[0];
      Map<Class<?>, Object> scenarioArgs = Maps.newHashMap(env.getScenarioArguments());
      scenarioArgs.keySet().retainAll(_subtreeTypes);
      Environment newEnv = env.withScenarioArguments(scenarioArgs);
      keyArgs[0] = newEnv;
      return keyArgs;
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
