/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link;

import com.opengamma.service.ServiceContext;

/**
 * A thread-local holder for a ServiceContext accessible across the system.
 */
public class ThreadLocalServiceContext {
  
  private static ThreadLocal<ServiceContext> s_instance;
  
  public static final void init(ServiceContext serviceContext) {
    s_instance = new ThreadLocal<ServiceContext>();
    s_instance.set(serviceContext);
  }
  
  public static final ServiceContext getInstance() {
    return s_instance.get();
  }
}
