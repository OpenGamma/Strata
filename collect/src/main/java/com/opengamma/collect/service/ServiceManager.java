/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import com.google.common.collect.ImmutableClassToInstanceMap;

/**
 * Manager of global application services provided by a thread-local.
 * <p>
 * This manager is used by applications to obtain access to a set of services.
 * For the purpose of this class, a service can be any thread-safe Java object.
 * A service is obtained using the service type, which is a {@code Class}, typically an interface.
 * <p>
 * The manager is typically initialized by the framework surrounding the application.
 * As such, an application does not normally need to initialize the manager.
 * It is always possible to obtain a context, however it may be empty if not initialized.
 * 
 * <h4>Usage</h4>
 * A thread-local instance of the context is obtained using {@code getContext()}:
 * <pre>
 *   ServiceContext context = ServiceManager.getContext();
 * </pre>
 * The context should be re-obtained by calling this method every time it is needed.
 * Caching the context or passing it around via method parameters is strongly discouraged.
 * 
 * <h4>Design note</h4>
 * The class effectively manages a form of shared global state.
 * While it is easy to say that global state is a Bad Thing, in practice it can be
 * a very important design tool to simplify API usage.
 * Alternative designs usually require application users to pass around and manage
 * objects that the framework could manage on behalf of the user.
 */
public final class ServiceManager {

  /**
   * The default service context.
   */
  private static volatile ServiceContext DEFAULT = ServiceContext.of(ImmutableClassToInstanceMap.builder().build());
  /**
   * The thread-local service context.
   */
  private static final ThreadLocal<ServiceContext> THREAD_LOCAL = new InheritableThreadLocal<ServiceContext>();

  //-------------------------------------------------------------------------
  /**
   * Gets the service context applicable to this thread.
   * <p>
   * This method is intended to be used within application code.
   * It returns the service context that has been initialized for this thread.
   * <p>
   * It is bad practice to retain a reference to the context, or pass it around
   * within application code. Instead, the context should always be re-obtained
   * by calling this method.
   * <p>
   * If no context has been initialized for this thread, a default instance is returned,
   * see {@link #addServiceToDefault(Class, Object)}.
   * Use of the default is a useful convenience intended primarily for a proof of concept environment.
   * Best practice is to run in a framework environment that correctly initializes a real context.
   * 
   * @return the context
   */
  public static ServiceContext getContext() {
    // note that the default must be substituted here
    // using ThreadLocal.initialValue() would lock a specific default immutable instance to a thread
    // as such, the thread would not pickup any subsequent alterations, such as from static initializers
    ServiceContext context = THREAD_LOCAL.get();
    if (context == null) {
      context = DEFAULT;
    }
    return context;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the service context applicable to this thread.
   * <p>
   * This method is intended to be used by frameworks and initialization code.
   * It sets the context accessible from the current thread.
   * 
   * @param serviceContext  the context to use for this thread
   */
  public static void init(ServiceContext serviceContext) {
    THREAD_LOCAL.set(serviceContext);
  }

  /**
   * Clears the service context from this thread.
   * <p>
   * This method is intended to be used by frameworks and initialization code.
   * It removes any current context from the current thread.
   */
  public static void clear() {
    THREAD_LOCAL.remove();
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a service to the default instance, intended for proof of concept use only.
   * <p>
   * In most cases, applications should not call this method.
   * It adds a service to the default context, which is intended for simple
   * proof of concept use only.
   * In general, the service context should be created and managed by a framework
   * and initialized on each thread using {@link #init(ServiceContext)}.
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
  private ServiceManager() {
  }

}
