/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.proxy.InvocationHandlerFactory;

/**
 * Interface for classes that can decorate nodes in the graph.
 */
public abstract class NodeDecorator {

  /**
   * A node decorator that does not decorate the input node.
   */
  public static final NodeDecorator IDENTITY = new NodeDecorator() {
    @Override
    public FunctionModelNode decorateNode(FunctionModelNode node) {
      return node;
    }
  };

  /**
   * Restricted constructor.
   */
  protected NodeDecorator() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a node after optionally wrapping it in a proxy node.
   * <p>
   * If the factory doesn't insert a proxy it must return the original node.
   * Proxy instances can only be created using {@link #createProxyNode}.
   * 
   * @param node  a node, not null
   * @return a node, possibly wrapped in a proxy, not null
   */
  public abstract FunctionModelNode decorateNode(FunctionModelNode node);

  /**
   * Creates a proxy node.
   * 
   * @param node  the underlying concrete node, not null
   * @param interfaceType  the expected type of the object created by this node, not null
   * @param implementationType  the implementation type to create, may be null
   * @param handlerFactory  the proxy invocation factory, not null
   * @return a new proxy node, not null
   */
  protected ProxyNode createProxyNode(FunctionModelNode node, Class<?> interfaceType, Class<?> implementationType, InvocationHandlerFactory handlerFactory) {
    return new ProxyNode(node, interfaceType, implementationType, handlerFactory);
  }

}
