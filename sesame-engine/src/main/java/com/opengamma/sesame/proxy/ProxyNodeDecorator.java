/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.opengamma.sesame.graph.FunctionModelNode;
import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.graph.ProxyNode;

/**
 * Decorates node in the graph with a proxy.
 * <p>
 * Subclasses should be stateless and thread safe as there is only one instance shared between all proxied objects.
 */
public abstract class ProxyNodeDecorator extends NodeDecorator implements InvocationHandlerFactory {

  @Override
  public FunctionModelNode decorateNode(FunctionModelNode node) {
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
      return createProxyNode(node, interfaceType, implementationType, this);
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

  //-------------------------------------------------------------------------
  /**
   * Indicates whether a node should be wrapped in a proxy.
   * <p>
   * Note that the implementation type is not necessarily the type of the delegate
   * as the delegate could be another proxy, ie. it is the type of the real object.
   *
   * @param interfaceType  the type of the interface being decorated, not null
   * @param implementationType  the implementation type being decorated, not null
   * @return true if decoration should occur
   */
  protected abstract boolean decorate(Class<?> interfaceType, Class<?> implementationType);

  /**
   * Called when a method on the proxy is invoked.
   * 
   * @param proxy  the proxy whose method was invoked, not null
   * @param delegate  the object being proxied, not null
   * @param method  the method that was invoked, not null
   * @param args  the method arguments, null if the method takes
   * no arguments
   * @return the return value of the call
   * @throws Throwable if something goes wrong with the underlying call
   * TODO param for the concrete type? or the proxy node itself?
   */
  protected abstract Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable;

}
