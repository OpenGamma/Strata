/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationHandler;

/**
 * Handler that is invoked when methods are called on proxies in the function graph.
 */
public interface ProxyInvocationHandler extends InvocationHandler {

  /**
   * Gets the receiver object.
   * 
   * @return the object to which the handler delegates method calls, may
   * be a proxy instance, not null
   */
  Object getReceiver();

  /**
   * Gets the receiver object ignoring any proxies.
   *
   * @return the object to which the handler delegates method calls once
   * all proxies have executed, not null
   */
  Object getProxiedObject();
}
