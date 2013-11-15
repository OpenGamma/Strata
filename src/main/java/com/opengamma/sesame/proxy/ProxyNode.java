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
 * Proxy nodes are effectively transparent to equality and hash code, i.e. only the real nodes are included.
 * This is valid as long as we don't have any proxies that change the behaviour or return values of nodes. If that
 * happens we'll have to have a rethink.
 */
public class ProxyNode extends DependentNode {

  private final Class<?> _interfaceType;
  private final Class<?> _implementationType;
  private final Node _delegateNode;
  private final InvocationHandlerFactory _handlerFactory;

  public ProxyNode(Node delegateNode,
                   Class<?> interfaceType,
                   Class<?> implementationType,
                   InvocationHandlerFactory handlerFactory) {
    super(delegateNode.getParameter(), delegateNode);
    _implementationType = ArgumentChecker.notNull(implementationType, "implementationType");
    _delegateNode = ArgumentChecker.notNull(delegateNode, "delegate");
    _interfaceType = ArgumentChecker.notNull(interfaceType, "interfaceType");
    _handlerFactory = ArgumentChecker.notNull(handlerFactory, "handlerFactory");
  }

  @Override
  public Object create(ComponentMap componentMap, List<Object> dependencies) {
    // TODO can I use ProxyGenerator here? or extract its logic?
    // TODO which class loader?
    Object delegate = dependencies.get(0);
    InvocationHandler invocationHandler = _handlerFactory.create(delegate, this);
    return Proxy.newProxyInstance(_interfaceType.getClassLoader(), new Class<?>[]{_interfaceType}, invocationHandler);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "proxy " + _interfaceType.getSimpleName() + "(" + _handlerFactory.getClass().getSimpleName() + ")";
  }

  public Node getDelegate() {
    return _delegateNode;
  }

  public Class<?> getInterfaceType() {
    return _interfaceType;
  }

  public Class<?> getImplementationType() {
    return _implementationType;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_interfaceType, _implementationType, _delegateNode, _handlerFactory);
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
        Objects.equals(this._interfaceType, other._interfaceType) &&
        Objects.equals(this._implementationType, other._implementationType) &&
        Objects.equals(this._delegateNode, other._delegateNode) &&
        Objects.equals(this._handlerFactory, other._handlerFactory);
  }
}
