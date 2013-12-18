/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.graph.Node;
import com.opengamma.sesame.graph.NodeDecorator;

/**
 * Decorates node in the graph with a proxy.
 * Subclasses should be stateless and thread safe as there is only one instance shared between all proxied objects.
 */
public abstract class ProxyNodeDecorator implements NodeDecorator, InvocationHandlerFactory {

  @Override
  public Node decorateNode(Node node) {
    if (!(node instanceof ProxyNode) && !(node instanceof InterfaceNode)) {
      return node;
    }
    Class<?> interfaceType;
    Class<?> implementationType;
    if (node instanceof InterfaceNode) {
      implementationType = ((InterfaceNode) node).getImplementationType();
      interfaceType = ((InterfaceNode) node).getType();
    } else {
      implementationType = ((ProxyNode) node).getImplementationType();
      interfaceType = ((ProxyNode) node).getType();
    }
    if (decorate(interfaceType, implementationType)) {
      return new ProxyNode(node, interfaceType, implementationType, this);
    } else {
      return node;
    }
  }

  @Override
  public ProxyInvocationHandler create(final Object delegate, ProxyNode node) {
    return new AbstractProxyInvocationHandler(delegate) {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
          return ProxyNodeDecorator.this.invoke(proxy, delegate, method, args);
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }
    };
  }

  /**
   * Indicates whether a node should be wrapped in a proxy.
   *
   * @param interfaceType The type of the interface being decorated
   * @param implementationType The implementation type being decorated. This isn't necessarily the type of the
   * delegate as the delegate could be another proxy. This is the type of the real object.
   */
  protected abstract boolean decorate(Class<?> interfaceType, Class<?> implementationType);

  /**
   * Called when a method on the proxy is invoked.
   * @param proxy The proxy whose method was invoked
   * @param delegate The object being proxied
   * @param method The method that was invoked
   * @param args The method arguments
   * @return The return value of the call
   * @throws Exception If something goes wrong with the underlying call
   * TODO param for the concrete type? or the proxy node itself?
   */
  protected abstract Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable;

}
