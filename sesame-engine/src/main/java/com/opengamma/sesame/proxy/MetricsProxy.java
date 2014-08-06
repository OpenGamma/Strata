/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.Method;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.util.metric.OpenGammaMetricRegistry;

/**
 * A proxy that measured the time taken by each method call and
 * sends it to the metrics repository.
 */
public final class MetricsProxy extends ProxyNodeDecorator {

  /**
   * Singleton instance of the timing proxy.
   */
  public static final MetricsProxy INSTANCE = new MetricsProxy();

  private final MetricRegistry _metricRegistry = OpenGammaMetricRegistry.getSummaryInstance();

  /**
   * Restricted constructor.
   */
  private MetricsProxy() {
  }

  //-------------------------------------------------------------------------
  @Override
  protected boolean decorate(Class<?> interfaceType, Class<?> implementationType) {
    return true;
  }

  @Override
  protected Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable {
    // this avoids timing calls to toString(),
    // hashCode() etc especially in a debugger
    if (method.getDeclaringClass() != Object.class) {
      String name = generateName(delegate, method);
      try (Timer.Context ignored = _metricRegistry.timer(name).time()) {
        return method.invoke(delegate, args);
      }
    } else {
      return method.invoke(delegate, args);
    }
  }

  private String generateName(Object delegate, Method method) {
    Object proxiedObject = EngineUtils.getProxiedObject(delegate);
    return proxiedObject.getClass().getCanonicalName() + "." + method.getName();
  }

}
