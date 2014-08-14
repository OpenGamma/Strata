/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.opengamma.collect.ArgChecker;

/**
 * A {@code ScheduledExecutorService} that ensures the correct service context map is initialized on each thread.
 * <p>
 * This executor decorates an original executor service to make it aware of {@link ServiceContext}.
 * It ensures that the {@link ServiceContext} thread-local is initialized for each task that is processed.
 */
public class ServiceContextAwareScheduledExecutorService
    extends ServiceContextAwareExecutorService
    implements ScheduledExecutorService {

  /**
   * The delegate scheduled executor.
   */
  private final ScheduledExecutorService delegate;

  /**
   * Creates an instance that decorates to another executor.
   * 
   * @param delegate  the underlying delegate that actually executes tasks
   */
  public ServiceContextAwareScheduledExecutorService(ScheduledExecutorService delegate) {
    super(delegate);
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  //-------------------------------------------------------------------------
  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return delegate.schedule(decorate(command), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return delegate.schedule(decorate(callable), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return delegate.scheduleAtFixedRate(decorate(command), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return delegate.scheduleWithFixedDelay(decorate(command), initialDelay, delay, unit);
  }

}
