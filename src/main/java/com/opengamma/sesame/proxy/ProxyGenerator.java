/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.opengamma.util.ArgumentChecker;

/**
 * Generates a proxy which
 */
public class ProxyGenerator {

  @SuppressWarnings("unchecked")
  public <T> T generate(final T delegate, Class<T> iface) {

    ArgumentChecker.notNull(delegate, "delegate");
    ArgumentChecker.notNull(iface, "iface");
    ArgumentChecker.isTrue(iface.isInterface(), "Can only generate proxies for interfaces");

    Class<?> delegateClass = delegate.getClass();

    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(delegate, args);
      }
    };

    return (T) Proxy.newProxyInstance(delegateClass.getClassLoader(), new Class[] {iface}, handler);
  }
}
