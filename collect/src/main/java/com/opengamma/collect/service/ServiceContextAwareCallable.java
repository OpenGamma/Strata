/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import java.util.concurrent.Callable;

import com.opengamma.collect.ArgChecker;

/**
 * A {@code Callable} decorator that ensures the thread-local service context map is present.
 */
final class ServiceContextAwareCallable<V>
    implements Callable<V> {

  /**
   * The service context map.
   */
  private final ServiceContextMap serviceContextMap;
  /**
   * The delegate.
   */
  private final Callable<V> delegate;

  /**
   * Creates an instance.
   * 
   * @param serviceContextMap  the service context map
   * @param delegate  the delegate callable
   */
  ServiceContextAwareCallable(ServiceContextMap serviceContextMap, Callable<V> delegate) {
    this.serviceContextMap = ArgChecker.notNull(serviceContextMap, "serviceContextMap");
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public V call() throws Exception {
    try {
      ServiceContext.init(serviceContextMap);
      return delegate.call();
    } finally {
      ServiceContext.clear();
    }
  }

}
