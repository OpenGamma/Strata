/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.opengamma.collect.ArgChecker;

/**
 * Manager of global application services provided by a thread-local.
 * <p>
 * This manager is used by applications to obtain access to a set of services.
 * For the purpose of this class, a service can be any thread-safe Java object.
 * A service is obtained using the service type, which is a {@code Class}, typically an interface.
 * <p>
 * The manager is typically initialized by the framework surrounding the application.
 * As such, an application does not normally need to initialize the manager.
 * It is always possible to obtain a context map, however it may be empty if not initialized.
 * 
 * <h4>Usage</h4>
 * A thread-local instance of the context map is obtained using {@code getMap()}:
 * <pre>
 *   FooService foo = ServiceContext.getMap().get(FooService.class);
 * </pre>
 * The context map should be re-obtained by calling this method every time it is needed.
 * Caching the context map or passing it around via method parameters is strongly discouraged.
 * 
 * <h4>Design note</h4>
 * The class effectively manages a form of shared global state.
 * While it is easy to say that global state is a Bad Thing, in practice it can be
 * a very important design tool to simplify API usage.
 * Alternative designs usually require application users to pass around and manage
 * objects that the framework could manage on behalf of the user.
 */
public final class ServiceContext {

  /**
   * The default service context map.
   */
  private static volatile ServiceContextMap DEFAULT = ServiceContextMap.of(ImmutableClassToInstanceMap.builder().build());
  /**
   * The thread-local service context map.
   */
  private static final ThreadLocal<ServiceContextMap> THREAD_LOCAL = new InheritableThreadLocal<ServiceContextMap>();

  //-------------------------------------------------------------------------
  /**
   * Gets the service context map applicable to this thread.
   * <p>
   * This method is intended to be used within application code.
   * It returns the service context map that has been initialized for this thread.
   * <p>
   * It is bad practice to retain a reference to the context map, or pass it around
   * within application code. Instead, the context map should always be re-obtained
   * by calling this method.
   * <p>
   * If no context map has been initialized for this thread, a default instance is returned,
   * see {@link #addServiceToDefault(Class, Object)}.
   * Use of the default is a useful convenience intended primarily for a proof of concept environment.
   * Best practice is to run in a framework environment that correctly initializes a real context map.
   * 
   * @return the context map
   */
  public static ServiceContextMap getMap() {
    // note that the default must be substituted here
    // using ThreadLocal.initialValue() would lock a specific default immutable instance to a thread
    // as such, the thread would not pickup any subsequent alterations, such as from static initializers
    ServiceContextMap context = THREAD_LOCAL.get();
    if (context == null) {
      context = DEFAULT;
    }
    return context;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the service context map applicable to this thread.
   * <p>
   * This method is intended to be used by frameworks and initialization code.
   * It sets the context map accessible from the current thread.
   * 
   * @param serviceContextMap  the context map to use for this thread
   */
  public static void init(ServiceContextMap serviceContextMap) {
    ArgChecker.notNull(serviceContextMap, "serviceContextMap");
    THREAD_LOCAL.set(serviceContextMap);
  }

  /**
   * Clears the service context map from this thread.
   * <p>
   * This method is intended to be used by frameworks and initialization code.
   * It removes any current context map from the current thread.
   */
  public static void clear() {
    THREAD_LOCAL.remove();
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a service to the default instance, intended for proof of concept use only.
   * <p>
   * In most cases, applications should not call this method.
   * It adds a service to the default context map, which is intended for simple
   * proof of concept use only.
   * In general, the service context map should be created and managed by a framework
   * and initialized on each thread using {@link #init(ServiceContextMap)}.
   * <p>
   * If the service type has already been registered, an exception will be thrown.
   * 
   * @param <T> the type being added
   * @param serviceType  the type of the service being added
   * @param service  the service instance to associate with the service type
   * @throws IllegalArgumentException if the service type is already present
   */
  public static <T> void addServiceToDefault(Class<T> serviceType, T service) {
    DEFAULT = DEFAULT.withAdded(serviceType, service);
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private ServiceContext() {
  }

}
