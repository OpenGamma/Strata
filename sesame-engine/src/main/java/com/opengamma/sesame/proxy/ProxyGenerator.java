/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Generates a proxy matching an interface.
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
        try {
          return method.invoke(delegate, args);
        } catch (InvocationTargetException ex) {
          // We got an exception from the underlying call but the proxy mechanism
          // automatically wraps it, so pull it out
          Throwable cause = ex.getCause();
          if (method.getReturnType() == Result.class) {
            return Result.failure(FailureStatus.ERROR, "Received exception: {}", cause);
          } else {
            throw cause;
          }
        }
      }
    };

    return (T) Proxy.newProxyInstance(delegateClass.getClassLoader(), new Class[] {iface}, handler);
  }

}
