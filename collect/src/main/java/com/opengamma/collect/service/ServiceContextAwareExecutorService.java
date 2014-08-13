/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.opengamma.collect.ArgChecker;

/**
 * An {@code ExecutorService} that ensures the correct service context is initialized on each thread.
 * <p>
 * This executor decorates an original executor service to make it aware of {@link ServiceManager}.
 * It ensures that the {@link ServiceManager} thread-local is initialized for each task that is processed.
 */
public class ServiceContextAwareExecutorService
    implements ExecutorService {

  /**
   * The delegate executor.
   */
  private final ExecutorService delegate;

  /**
   * Creates an instance that decorates to another executor.
   * 
   * @param delegate  the underlying delegate that actually executes tasks
   */
  public ServiceContextAwareExecutorService(ExecutorService delegate) {
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(decorate(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(decorate(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(decorate(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(decorate(tasks));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return delegate.invokeAll(decorate(tasks), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return delegate.invokeAny(decorate(tasks));
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(decorate(tasks), timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(decorate(command));
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  //-------------------------------------------------------------------------
  // decorates a runnable
  ServiceContextAwareRunnable decorate(Runnable task) {
    return new ServiceContextAwareRunnable(ServiceManager.getContext(), task);
  }

  // decorates a callable
  <T> ServiceContextAwareCallable<T> decorate(Callable<T> task) {
    return new ServiceContextAwareCallable<>(ServiceManager.getContext(), task);
  }

  // decorates a list of callables
  <T> List<Callable<T>> decorate(Collection<? extends Callable<T>> tasks) {
    ServiceContext context = ServiceManager.getContext();
    List<Callable<T>> taskList = new ArrayList<>(tasks.size());
    for (Callable<T> task : tasks) {
      taskList.add(new ServiceContextAwareCallable<>(context, task));
    }
    return taskList;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ServiceContextAware[" + delegate + "]";
  }

}
