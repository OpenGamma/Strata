/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import com.opengamma.collect.ArgChecker;

/**
 * A {@code Runnable} decorator that ensures the thread-local service context map is present.
 */
final class ServiceContextAwareRunnable
    implements Runnable {

  /**
   * The service context map.
   */
  private final ServiceContextMap serviceContextMap;
  /**
   * The delegate.
   */
  private final Runnable delegate;

  /**
   * Creates an instance.
   * 
   * @param serviceContextMap  the service context map
   * @param delegate  the delegate runnable
   */
  ServiceContextAwareRunnable(ServiceContextMap serviceContextMap, Runnable delegate) {
    this.serviceContextMap = ArgChecker.notNull(serviceContextMap, "serviceContextMap");
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public void run() {
    try {
      ServiceContext.init(serviceContextMap);
      delegate.run();
    } finally {
      ServiceContext.clear();
    }
  }

}
