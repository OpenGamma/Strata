/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.graph.Node;
import com.opengamma.util.ArgumentChecker;

/**
 * A graph model node that inserts a dynamic proxy in front of a component.
 */
public class ProxyNode extends Node {

  private final Class<?> _interfaceType;
  private final Class<?> _implementationType;
  private final Node _delegateNode;
  private final InvocationHandlerFactory _handlerFactory;
  private final List<Node> _dependencies;

  public ProxyNode(Node delegateNode,
                   Class<?> interfaceType,
                   Class<?> implementationType,
                   InvocationHandlerFactory handlerFactory) {
    super(delegateNode.getParameter());
    _implementationType = ArgumentChecker.notNull(implementationType, "implementationType");
    _delegateNode = ArgumentChecker.notNull(delegateNode, "delegate");
    _interfaceType = ArgumentChecker.notNull(interfaceType, "interfaceType");
    _handlerFactory = ArgumentChecker.notNull(handlerFactory, "handlerFactory");
    _dependencies = Collections.singletonList(delegateNode);
  }

  @Override
  public Object create(ComponentMap componentMap) {
    // TODO can I use ProxyGenerator here? or extract its logic?
    // TODO which class loader?
    Object delegate = _delegateNode.create(componentMap);
    InvocationHandler invocationHandler = _handlerFactory.create(delegate, this);
    return Proxy.newProxyInstance(_interfaceType.getClassLoader(), new Class<?>[]{_interfaceType}, invocationHandler);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "proxy " + _interfaceType.getSimpleName() + "(" + _handlerFactory.getClass().getSimpleName() + ")";
  }

  @Override
  public List<Node> getDependencies() {
    return _dependencies;
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
}
