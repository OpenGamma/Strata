/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.collect.ArgChecker;

/**
 * Context map providing services keyed by type, normally obtained via {@code ServiceContext}.
 * <p>
 * A single context map holds a map of services keyed by a {@code Class} known as the service type.
 * For the purpose of this class, a service can be any thread-safe Java object.
 * <p>
 * Services are obtained using the static method {@link ServiceContext#getMap()}.
 * That method operates using a thread-local model, allowing different threads to have different context maps.
 * 
 * <h4>Usage</h4>
 * A thread-local instance of the context map is obtained using {@code ServiceManager}
 * and then queried using {@code get(Class)}:
 * <pre>
 *   FooService foo = ServiceContext.get().get(FooService.class);
 * </pre>
 * The context map should be re-obtained from the {@code ServiceContext} every time it is needed.
 * Caching the context map or passing it around via method parameters is strongly discouraged.
 */
public final class ServiceContextMap {

  /**
   * The services held by this context map.
   */
  private ImmutableClassToInstanceMap<Object> services;

  //-------------------------------------------------------------------------
  /**
   * Obtains a service context map using the specified map of services.
   * <p>
   * The map consists of services keyed by the service type.
   * This must follow the principles of {@link ClassToInstanceMap}.
   * 
   * @param services  a map of type to service-providing objects
   * @return the service context map
   */
  public static ServiceContextMap of(Map<Class<?>, Object> services) {
    ArgChecker.noNulls(services, "services");
    return new ServiceContextMap(ImmutableClassToInstanceMap.copyOf(services));
  }

  /**
   * Obtains a service context map containing a single service.
   * <p>
   * The resulting context map contains the single specified service keyed by the type.
   * Typically this context map would then be augmented using the {@code with} method,
   * effectively acting as a builder.
   * 
   * @param <T> the type being added
   * @param serviceType  the type of the service
   * @param service  a service-providing object
   * @return the service context map
   */
  public static <T> ServiceContextMap of(Class<T> serviceType, T service) {
    ArgChecker.notNull(serviceType, "serviceType");
    ArgChecker.notNull(service, "service");
    return new ServiceContextMap(ImmutableClassToInstanceMap.builder().put(serviceType, service).build());
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param services  the services to start with
   */
  private ServiceContextMap(ImmutableClassToInstanceMap<Object> services) {
    this.services = ArgChecker.notNull(services, "services");
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this context map contains the specified service type.
   * <p>
   * This checks to see if this context map contains an entry for the service type.
   * 
   * @param serviceType  the type of the service
   * @return true if the service is available
   */
  public boolean contains(Class<?> serviceType) {
    ArgChecker.notNull(serviceType, "serviceType");
    return services.containsKey(serviceType);
  }

  /**
   * Gets the service associated with the specified specified type.
   * <p>
   * This returns the service associated with the service type.
   * If the service type is not found, an exception is thrown.
   * 
   * @param <T>  the required service type
   * @param serviceType  the type of the service
   * @return the service
   * @throws IllegalArgumentException if the service is not found
   */
  public <T> T get(Class<T> serviceType) {
    ArgChecker.notNull(serviceType, "serviceType");
    final T service = services.getInstance(serviceType);
    if (service == null) {
      throw new IllegalArgumentException("No service found: " + serviceType);
    }
    return service;
  }

  /**
   * Finds the service associated with the specified specified type, returning null if not found.
   * <p>
   * This returns the service associated with the service type.
   * If the service type is not found, null is returned.
   * 
   * @param <T>  the required service type
   * @param serviceType  the type of the service
   * @return the service, null if not found
   */
  @Nullable
  public <T> T find(Class<T> serviceType) {
    ArgChecker.notNull(serviceType, "serviceType");
    return services.getInstance(serviceType);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this context map with the map of services added.
   * <p>
   * If any services are provided that are already registered, the service registry
   * will be updated with the provided services.
   * 
   * @param services  a map of services objects keyed by their class
   * @return an updated service context map
   */
  public ServiceContextMap with(Map<Class<?>, Object> services) {
    // have to calculate which of the original objects need to be retained as
    // ImmutableMap.Builder won't allow a key to be put more than once
    ArgChecker.noNulls(services, "services");
    Map<Class<?>, Object> combined = new HashMap<>(this.services);
    combined.putAll(services);
    return new ServiceContextMap(ImmutableClassToInstanceMap.copyOf(combined));
  }

  /**
   * Returns a copy of this context map with the specified service added.
   * <p>
   * The returned context map will consist of all the existing services plus the new one.
   * If the service type already exists, the specified service will replace the existing
   * one in the result.
   * 
   * @param serviceType  the type of the service being added
   * @param service  the service instance to associate with the service type
   * @return a copy of this context map with the new service added
   */
  public <T> ServiceContextMap with(Class<? extends T> serviceType, T service) {
    ArgChecker.notNull(serviceType, "serviceType");
    ArgChecker.notNull(service, "service");
    return with(ImmutableMap.of(serviceType, service));
  }

  /**
   * Returns a copy of this context map with the specified service added.
   * <p>
   * The returned context map will consist of all the existing services plus the new one.
   * An exception is thrown if the service type is already registered unless the
   * news service equals the old one.
   * 
   * @param <T> the type being added
   * @param serviceType  the type of the service being added
   * @param service  the service instance to associate with the service type
   * @return a copy of this context map with the new service added
   * @throws IllegalArgumentException if the service type is already associated with a service
   */
  <T> ServiceContextMap withAdded(Class<? extends T> serviceType, T service) {
    ArgChecker.notNull(serviceType, "serviceType");
    ArgChecker.notNull(service, "service");
    // slight leniency allows equal service to be registered more than once
    Object existing = services.get(serviceType);
    if (existing != null && existing.equals(service) == false) {
      throw new IllegalArgumentException(
          "Unable to add service as the type has already been associated with a service: " + serviceType.getSimpleName());
    }
    return with(ImmutableMap.of(serviceType, service));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the entire map of services.
   * <p>
   * This returns the immutable map of known services.
   * This is intended primarily for use by frameworks rather than applications.
   * 
   * @return the map of available services keyed by service type
   */
  public ImmutableClassToInstanceMap<Object> getServices() {
    return services;
  }

  /**
   * Gets the entire set of service types.
   * <p>
   * This returns the immutable set of known service types.
   * This is intended primarily for use by frameworks rather than applications.
   * 
   * @return the set of available service types
   */
  public ImmutableSet<Class<?>> getServiceTypes() {
    return ImmutableSet.copyOf(services.keySet());
  }

  //-------------------------------------------------------------------------
  /**
   * Runs the specified {@code Runnable} using this context map.
   * <p>
   * This is intended to be used to execute a lambda expression using this context map.
   * <pre>
   *   context.run(() -> {
   *     // code executed on this thread
   *     // where ServiceContext.getMap() returns this context
   *   });
   * </pre>
   * Execution occurs on this thread.
   * 
   * @param closure  the runnable to run using this context, typically a lambda expression
   */
  public void run(Runnable closure) {
    associateWith(closure).run();
  }

  /**
   * Associates the specified {@code Runnable} with this context map.
   * <p>
   * This can be used to associate a task with this context map.
   * Calling this method is necessary to ensure that the task has the correct
   * context map if it is run on a different thread.
   * In general, it is recommended to use {@link ServiceContextAwareExecutorService}
   * instead of calling this method.
   * 
   * @param runnable  the runnable to associate
   * @return the decorated runnable
   */
  public Runnable associateWith(Runnable runnable) {
    return new ServiceContextAwareRunnable(this, runnable);
  }

  /**
   * Associates the specified {@code Callable} with this context map.
   * <p>
   * This can be used to associate a task with this context map.
   * Calling this method is necessary to ensure that the task has the correct
   * context map if it is run on a different thread.
   * In general, it is recommended to use {@link ServiceContextAwareExecutorService}
   * instead of calling this method.
   * 
   * @param callable  the callable to associate
   * @return the decorated callable
   */
  public <V> Callable<V> associateWith(Callable<V> callable) {
    return new ServiceContextAwareCallable<>(this, callable);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ServiceContextMap[size=" + services.size() + "]";
  }

}
