/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.graph.Node;
import com.opengamma.util.ArgumentChecker;

/**
 * A graph model node that inserts a dynamic proxy in front of a component.
 */
/* package */ class ProxyNode extends Node {

  private final Class<?> _interfaceType;
  private final Node _delegateNode;
  private final InvocationHandlerFactory _handlerFactory;

  /* package */ ProxyNode(Node delegateNode, Class<?> interfaceType, InvocationHandlerFactory handlerFactory) {
    _delegateNode = ArgumentChecker.notNull(delegateNode, "delegate");
    _interfaceType = ArgumentChecker.notNull(interfaceType, "interfaceType");
    _handlerFactory = ArgumentChecker.notNull(handlerFactory, "handlerFactory");
  }

  @Override
  public Object create(ComponentMap componentMap) {
    // TODO which class loader?
    Object delegate = _delegateNode.create(componentMap);
    InvocationHandler invocationHandler = _handlerFactory.create(delegate);
    return Proxy.newProxyInstance(_interfaceType.getClassLoader(), new Class<?>[]{_interfaceType}, invocationHandler);
  }
}
