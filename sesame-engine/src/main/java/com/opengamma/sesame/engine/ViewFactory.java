/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import com.opengamma.sesame.proxy.MetricsProxy;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;

/**
 * Factory for creating instances of {@link View}.
 * This is one of the key classes of the calculation engine.
 * The {@link #createView} methods take a view configuration
 * and returns a view that is ready to be executed.
 * <p>
 * Each view factory contains a cache which is shared by all
 * views it creates.
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
  private final Optional<MetricRegistry> _metricRegistry;

  public ViewFactory(ExecutorService executor,
                     AvailableOutputs availableOutputs,
                     AvailableImplementations availableImplementations,
                     FunctionModelConfig defaultConfig,
                     EnumSet<FunctionService> defaultServices,
                     CachingManager cachingManager) {
    this(executor, availableOutputs, availableImplementations, defaultConfig,
         defaultServices, cachingManager, Optional.<MetricRegistry>absent());
  }

  public ViewFactory(ExecutorService executor,
                     AvailableOutputs availableOutputs,
                     AvailableImplementations availableImplementations,
                     FunctionModelConfig defaultConfig,
                     EnumSet<FunctionService> defaultServices,
                     CachingManager cachingManager,
                     Optional<MetricRegistry> metricRegistry) {
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "availableImplementations");
    _defaultServices = ArgumentChecker.notNull(defaultServices, "defaultServices");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _executor = ArgumentChecker.notNull(executor, "executor");
    _cachingManager = ArgumentChecker.notNull(cachingManager, "cachingManager");
    _metricRegistry = ArgumentChecker.notNull(metricRegistry, "metricRegistry");
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
    ComponentMap componentMap = _cachingManager.getComponentMap();

    s_logger.debug("building graph model");
    GraphBuilder graphBuilder = new GraphBuilder(_availableOutputs,
                                                 _availableImplementations,
                                                 componentMap.getComponentTypes(),
                                                 _defaultConfig,
                                                 decorator);
    GraphModel graphModel = graphBuilder.build(viewConfig, inputTypes);

    s_logger.debug("graph model complete, building graph");
    Graph graph = graphModel.build(componentMap, _functionBuilder);
    s_logger.debug("graph complete");

    return new View(viewConfig, graph, _executor, _defaultConfig, _cachingManager, graphModel);
  }

  private NodeDecorator createNodeDecorator(EnumSet<FunctionService> services) {

    ImmutableList.Builder<NodeDecorator> decorators = new ImmutableList.Builder<>();

    // Build up the proxies to be used from the outermost
    // to the innermost

    // Timing/tracing sits outside of caching so the actual
    // time taken for a request is reported. This can also
    // report on whether came from the cache or were calculated
    if (services.contains(FunctionService.TIMING)) {
      decorators.add(TimingProxy.INSTANCE);
    }
    if (services.contains(FunctionService.TRACING)) {
      decorators.add(TracingProxy.INSTANCE);
    }

    // Caching proxy memoizes requests as required so that
    // expensive calculations are not performed more
    // frequently than they need to be
    if (services.contains(FunctionService.CACHING)) {
      decorators.add(_cachingManager.getCachingDecorator());
    }

    // Metrics records time taken to execute each function. This
    // sits inside the caching layer as we're interested in how
    // long the actual calculation takes not how long it takes to
    // get from the cache
    if (services.contains(FunctionService.METRICS)) {
      if (_metricRegistry.isPresent()) {
        decorators.add(MetricsProxy.of(_metricRegistry.get()));
      } else {
        // This should be prevented by the ViewFactoryComponentFactory but is
        // here in case or programmatic misconfiguration
        s_logger.warn("Unable to create metrics proxy as no metrics repository has been configured");
      }
    }

    // Ensure we always have the exception wrapping behaviour so
    // methods returning Result<?> return Failure if an exception
    // is thrown internally.
    decorators.add(ExceptionWrappingProxy.INSTANCE);
    return CompositeNodeDecorator.compose(decorators.build());
  }
}
