/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationHandler;

/**
 * Factory for objects that provide the behaviour of a proxy.
 * This class is necessary because node decorators are invoked when the graph model is built but
 * the handler can't be created until the graph is built and the object being proxied is created. This interface
 * provides a level of indirection that separates the node decoration from handler creation.
 */
public interface InvocationHandlerFactory {

  /**
   * Creates a handler for a proxy that sits in front of the delegate
   * @param delegate The object being proxied
   * @return A handler that provides the proxy behaviour
   */
  InvocationHandler create(Object delegate);
}
