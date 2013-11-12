/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.Method;

/**
 *
 */
public class TimingProxy extends ProxyNodeDecorator {

  public static final TimingProxy INSTANCE = new TimingProxy();

  private static ThreadLocal<Integer> _depth = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return -1;
    }
  };

  private TimingProxy() {
  }

  @Override
  protected boolean decorate(Class<?> interfaceType, Class<?> implementationType) {
    return true;
  }

  @Override
  protected Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable {
    long start = System.nanoTime();
    try {
      _depth.set(_depth.get() + 1);
      return method.invoke(delegate, args);
    } finally {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < _depth.get(); i++) {
        builder.append("  ");
      }
      builder
          .append((System.nanoTime() - start) / 1e6d)
          .append("ms ")
          .append(method.getDeclaringClass().getSimpleName())
          .append(".")
          .append(method.getName());
      System.out.println(builder);
      _depth.set(_depth.get() - 1);
    }
  }
}
