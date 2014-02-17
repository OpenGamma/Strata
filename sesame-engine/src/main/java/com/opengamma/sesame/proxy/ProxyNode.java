/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.graph.DependentNode;
import com.opengamma.sesame.graph.Node;
import com.opengamma.util.ArgumentChecker;

/**
 * A graph model node that inserts a dynamic proxy in front of a component.
 */
public class ProxyNode extends DependentNode {

  private final Class<?> _implementationType;
  private final Node _delegateNode;
  // TODO should this be a Class<?> and the instance can be retrieved from the ComponentMap? that could be serialized
  private final InvocationHandlerFactory _handlerFactory;

  public ProxyNode(Node delegateNode,
                   Class<?> interfaceType,
                   Class<?> implementationType,
                   InvocationHandlerFactory handlerFactory) {
    super(interfaceType, delegateNode.getParameter(), delegateNode);
    _implementationType = ArgumentChecker.notNull(implementationType, "implementationType");
    _delegateNode = ArgumentChecker.notNull(delegateNode, "delegate");
    _handlerFactory = ArgumentChecker.notNull(handlerFactory, "handlerFactory");
  }

  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    // TODO can I use ProxyGenerator here? or extract its logic?
    // TODO which class loader?
    // TODO if delegate is a proxy need to drill down and get the real receiver
    Object delegate = dependencies.get(0);
    InvocationHandler invocationHandler = _handlerFactory.create(delegate, this);
    return Proxy.newProxyInstance(getType().getClassLoader(), new Class<?>[]{getType()}, invocationHandler);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "proxy " + getType().getSimpleName() + "(" + _handlerFactory.getClass().getSimpleName() + ")";
  }

  public Node getDelegate() {
    return _delegateNode;
  }

  public Class<?> getImplementationType() {
    return _implementationType;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_implementationType, _delegateNode, _handlerFactory);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ProxyNode other = (ProxyNode) obj;
    return
        Objects.equals(this._implementationType, other._implementationType) &&
        Objects.equals(this._delegateNode, other._delegateNode) &&
        Objects.equals(this._handlerFactory, other._handlerFactory);
  }
}
