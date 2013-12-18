/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationHandler;

/**
 * Invoked when methods are called on proxies in the function graph.
 */
public interface ProxyInvocationHandler extends InvocationHandler {

  /**
   * @return The object to which the handler delegates method calls. May be a proxy instance.
   */
  Object getReceiver();
}
