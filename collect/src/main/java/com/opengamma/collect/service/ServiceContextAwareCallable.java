/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import java.util.concurrent.Callable;

import com.opengamma.collect.ArgChecker;

/**
 * A {@code Runnable} decorator that ensures the service context is present.
 */
final class ServiceContextAwareCallable<V>
    implements Callable<V> {

  /**
   * The service context.
   */
  private final ServiceContext serviceContext;
  /**
   * The delegate.
   */
  private final Callable<V> delegate;

  /**
   * Creates an instance
   * 
   * @param serviceContext  the service context
   * @param delegate  the delegate callable
   */
  ServiceContextAwareCallable(ServiceContext serviceContext, Callable<V> delegate) {
    this.serviceContext = ArgChecker.notNull(serviceContext, "serviceContext");
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public V call() throws Exception {
    try {
      ServiceManager.init(serviceContext);
      return delegate.call();
    } finally {
      ServiceManager.clear();
    }
  }

}
