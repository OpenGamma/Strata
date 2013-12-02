/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.Method;

/**
 * TODO print the implementing class, not the interface type
 * TODO more sophisticated - selectively enable for specific inputs or threads
 * TODO log rather than printing to stdout
 * TODO include thread name so output can be interleaved
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
      /*StringBuilder entryBuilder = new StringBuilder();
      indent(entryBuilder);
      entryBuilder
          .append(method.getDeclaringClass().getSimpleName())
          .append(".")
          .append(method.getName());
      System.out.println(entryBuilder);*/
      return method.invoke(delegate, args);
    } finally {
      StringBuilder exitBuilder = new StringBuilder();
      indent(exitBuilder);
      exitBuilder
          .append((System.nanoTime() - start) / 1e6d)
          .append("ms ")
          .append(method.getDeclaringClass().getSimpleName())
          .append(".")
          .append(method.getName());
      System.out.println(exitBuilder);
      _depth.set(_depth.get() - 1);
    }
  }

  private void indent(StringBuilder builder) {
    for (int i = 0; i < _depth.get(); i++) {
      builder.append("  ");
    }
  }
}
