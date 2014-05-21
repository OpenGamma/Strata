/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphBuilder;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.proxy.ExceptionWrappingProxy;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;

/**
 * Factory for creating instances of {@link View}.
 * This is one of the key classes of the calculation engine. The {@link #createView} methods take a view configuration
 * and returns a view that is ready to be executed.
 * <p>
 * Each view factory contains a cache which is shared by all views it creates.
 */
public class ViewFactory {

  private static final Logger s_logger = LoggerFactory.getLogger(ViewFactory.class);

  private final ExecutorService _executor;
  private final AvailableOutputs _availableOutputs;
  private final AvailableImplementations _availableImplementations;
  private final EnumSet<FunctionService> _defaultServices;
  private final FunctionModelConfig _defaultConfig;
  private final FunctionBuilder _functionBuilder = new FunctionBuilder();
  private final CachingManager _cachingManager;

  public ViewFactory(ExecutorService executor,
                     AvailableOutputs availableOutputs,
                     AvailableImplementations availableImplementations,
                     FunctionModelConfig defaultConfig,
                     EnumSet<FunctionService> defaultServices,
                     CachingManager cachingManager) {
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "availableImplementations");
    _defaultServices = ArgumentChecker.notNull(defaultServices, "defaultServices");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _executor = ArgumentChecker.notNull(executor, "executor");
    _cachingManager = ArgumentChecker.notNull(cachingManager, "cachingManager");
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   * 
   * @param viewConfig  the configuration to use, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, Set<Class<?>> inputTypes) {
    return createView(viewConfig, _defaultServices, inputTypes);
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   *
   * @param viewConfig  the configuration to use, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, Class<?>... inputTypes) {
    return createView(viewConfig, _defaultServices, inputTypes);
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   * 
   * @param viewConfig  the configuration to use, not null
   * @param services  the services to run, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, EnumSet<FunctionService> services, Class<?>... inputTypes) {
    return createView(viewConfig, services, ImmutableSet.copyOf(inputTypes));
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   *
   * @param viewConfig  the configuration to use, not null
   * @param services  the services to run, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, EnumSet<FunctionService> services, Set<Class<?>> inputTypes) {

    NodeDecorator decorator = createNodeDecorator(services);
    ProxiedComponentMap listener = new DefaultProxiedComponentMap();

    // The component map has its components wrapped so they can
    // used for cache invalidation when required. It will receive
    // callbacks from all views which is correct as the cache spans
    // all views but is not what is required here. We need a
    // component map that only receives calls from this view so we
    // need to wrap again
    ComponentMap wrappedComponents = wrap(_cachingManager.getComponentMap(), listener);

    s_logger.debug("building graph model");
    GraphBuilder graphBuilder = new GraphBuilder(_availableOutputs,
                                                 _availableImplementations,
                                                 wrappedComponents.getComponentTypes(),
                                                 _defaultConfig,
                                                 decorator);
    GraphModel graphModel = graphBuilder.build(viewConfig, inputTypes);

    s_logger.debug("graph model complete, building graph");
    Graph graph = graphModel.build(wrappedComponents, _functionBuilder);
    s_logger.debug("graph complete");

    return new View(viewConfig, graph, _executor, _defaultConfig, _cachingManager, graphModel, listener);
  }

  // TODO - this could maybe go on component map itself
  private ComponentMap wrap(ComponentMap components, final ProxiedComponentMap listener) {

    Map<Class<?>, Object> wrapped = new HashMap<>();

    // Wrap each component in the map with a proxy, which will
    // automatically notify the listener whenever requests are
    // made from one of the components
    for (Map.Entry<Class<?>, Object> entry : components.getComponents().entrySet()) {

      final Class<?> key = entry.getKey();
      final Object component = entry.getValue();

      // This proxy mechanism works correctly but unfortunately the
      // sources and other components in proxies are not so well behaved
      // so we won't get consistent data recorded
      Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class<?>[]{key}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

          Object result = method.invoke(component, args);
          if (result != null) {
            listener.receivedCall(key, result);
          }
          return result;
        }
      });
      wrapped.put(key, proxy);
    }

    return ComponentMap.of(wrapped);
  }

  private NodeDecorator createNodeDecorator(EnumSet<FunctionService> services) {

    // Ensure we always have the exception wrapping behaviour
    if (services.isEmpty()) {
      return ExceptionWrappingProxy.INSTANCE;
    } else {
      List<NodeDecorator> decorators = Lists.newArrayListWithCapacity(services.size());

      if (services.contains(FunctionService.CACHING)) {
        decorators.add(_cachingManager.getCachingDecorator());
      }
      if (services.contains(FunctionService.TIMING)) {
        decorators.add(TimingProxy.INSTANCE);
      }
      if (services.contains(FunctionService.TRACING)) {
        decorators.add(TracingProxy.INSTANCE);
      }
      decorators.add(ExceptionWrappingProxy.INSTANCE);
      return CompositeNodeDecorator.compose(decorators);
    }
  }
}
