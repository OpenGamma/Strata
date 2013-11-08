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
  private final Node _delegateNode;
  private final InvocationHandlerFactory _handlerFactory;
  private final List<Node> _dependencies;

  /* package */ ProxyNode(Node delegateNode, Class<?> interfaceType, InvocationHandlerFactory handlerFactory) {
    super(delegateNode.getParameter());
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
    // TODO can I pass the concrete type in here?
    InvocationHandler invocationHandler = _handlerFactory.create(delegate);
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
}
