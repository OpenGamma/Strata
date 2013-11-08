/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.Method;

import com.opengamma.sesame.graph.InterfaceNode;

/**
 *
 */
public class TimingProxy extends ProxyNodeDecorator {

  public static final TimingProxy INSTANCE = new TimingProxy();

  private TimingProxy() {
  }

  @Override
  protected boolean decorate(InterfaceNode node) {
    return true;
  }

  @Override
  protected Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable {
    long start = System.nanoTime();
    try {
      return method.invoke(delegate, args);
    } finally {
      System.out.println(((System.nanoTime() - start) / 1e6d) + "ms " + method.getDeclaringClass().getSimpleName() +
                             "." + method.getName());
    }
  }
}
