/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.service;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.util.ArgumentChecker;

/**
 * A registry containing service-providing objects registered and retreived by their class
 */
public final class ServiceContext {
  private Map<Class<?>, ?> _services;
  
  private ServiceContext() {
    _services = new HashMap<Class<?>, Object>();
  }
  
  private ServiceContext(Map<Class<?>, Object> services) {
    // don't need to argument check as private
    _services = services;
  }
  
  /**
   * Gets the service-providing object of the specified type
   * @param <E> expected type
   * @param clazz the class of the service, not null
   * @return the service-providing object
   */
  @SuppressWarnings("unchecked")  
  public <E> E getService(Class<E> clazz) {
    ArgumentChecker.notNull(clazz, "class");
    return (E) _services.get(clazz);
  }
  
  /**
   * Add the services in the provided map to the registered services.  If any services are
   * provided that are already registered, the service registry will be updated with the provided 
   * services
   * @param services a map of services objects keyed by their class, not null
   * @return an updated service context
   */
  public ServiceContext with(Map<Class<?>, Object> services) {
    ArgumentChecker.notNull(services, "services");
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    return new ServiceContext(builder.putAll(_services).putAll(services).build());
  }

  /**
   * Add the service provided to the registered services.  If the service provided is already 
   * registered, the service registry will be updated with the provided service.
   * @param clazz the class of the service to be added, not null
   * @param service the service-providing object to be registered, not null
   * @return an updated service context
   */
  public ServiceContext with(Class<?> clazz, Object service) {
    ArgumentChecker.notNull(clazz, "class");
    ArgumentChecker.notNull(service, "service");
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    return new ServiceContext(builder.putAll(_services).put(clazz, service).build());
  }

  /**
   * Creates a new service context using the provided map to populate the service registry
   * @param services a map of type to service-providing objects, not null
   * @return a populated service context
   */
  public static ServiceContext of(Map<Class<?>, Object> services) {
    ArgumentChecker.notNull(services, "services");
    return new ServiceContext(ImmutableMap.copyOf(services));
  }
  
  /**
   * Creates a new service context using the single provided service and type.  Typically this
   * context would then be augmented using the @see with method.
   * @param clazz the class of the initial service being registered, not null
   * @param service a service-providing object, not null
   * @return a populated service context
   */
  public static ServiceContext of(Class<?> clazz, Object service) {
    ArgumentChecker.notNull(clazz, "class");
    ArgumentChecker.notNull(service, "service");
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    builder.put(clazz, service);
    return new ServiceContext(builder.build());
  }
  
  
}
