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
 * TODO this is fatally flawed. this is part of the node which means it must be serializable
 * but that's never going to work for any non-trivial proxy which needs to hook into the system (e.g. caching)
 * should the create() method take a ComponentMap?
 * could the proxy just have a ref to the type of the factory and get one from the component map?
 */
public interface InvocationHandlerFactory {

  /**
   * Creates a handler for a proxy that sits in front of the delegate
   *
   * @param delegate The object being proxied
   * @param node The proxy node
   * @return A handler that provides the proxy behaviour
   * TODO this should probably have an argument for the node too
   */
  InvocationHandler create(Object delegate, ProxyNode node);
}
