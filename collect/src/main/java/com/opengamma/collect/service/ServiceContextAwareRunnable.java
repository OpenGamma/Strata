/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import com.opengamma.collect.ArgChecker;

/**
 * A {@code Runnable} decorator that ensures the service context is present.
 */
final class ServiceContextAwareRunnable
    implements Runnable {

  /**
   * The service context.
   */
  private final ServiceContext serviceContext;
  /**
   * The delegate.
   */
  private final Runnable delegate;

  /**
   * Creates an instance
   * 
   * @param serviceContext  the service context
   * @param delegate  the delegate runnable
   */
  ServiceContextAwareRunnable(ServiceContext serviceContext, Runnable delegate) {
    this.serviceContext = ArgChecker.notNull(serviceContext, "serviceContext");
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public void run() {
    try {
      ServiceManager.init(serviceContext);
      delegate.run();
    } finally {
      ServiceManager.clear();
    }
  }

}
