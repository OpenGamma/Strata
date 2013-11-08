/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.proxy.ProxyNodeDecorator;
import com.opengamma.util.ArgumentChecker;

/**
 * Inserts a proxy in front of all interface nodes that records method calls, arguments and return values.
 */
public final class TracingProxy extends ProxyNodeDecorator {

  public static final TracingProxy INSTANCE = new TracingProxy();

  private static final ThreadLocal<Tracer> s_tracer = new ThreadLocal<Tracer>() {
    @Override
    protected Tracer initialValue() {
      return NoOpTracer.INSTANCE;
    }
  };

  private TracingProxy() {
  }

  @Override
  protected boolean decorate(InterfaceNode node) {
    return true;
  }

  @Override
  protected Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable {
    // this avoids recording calls to toString() in the debugger
    if (method.getName().equals("toString")) {
      try {
        return method.invoke(delegate, args);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }
    Tracer tracer = s_tracer.get();
    tracer.called(method, args);
    try {
      Object retVal = method.invoke(delegate, args);
      tracer.returned(retVal);
      return retVal;
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      tracer.threw(cause);
      throw cause;
    }
  }

  public static void start(Tracer tracer) {
    s_tracer.set(ArgumentChecker.notNull(tracer, "tracer"));
  }

  public static Call end() {
    Tracer tracer = s_tracer.get();
    s_tracer.remove();
    return tracer.getRoot();
  }
}
