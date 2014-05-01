/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.lang.reflect.Method;

import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.proxy.ProxyNodeDecorator;
import com.opengamma.util.ArgumentChecker;

/**
 * A proxy that records method calls, arguments and return values.
 * <p>
 * This can be used in front of all interface nodes.
 */
public final class TracingProxy extends ProxyNodeDecorator {

  /**
   * Singleton instance of the tracing proxy.
   */
  public static final TracingProxy INSTANCE = new TracingProxy();

  /**
   * The thread-local for the tracer.
   */
  private static final ThreadLocal<Tracer> s_tracer = new ThreadLocal<Tracer>() {
    @Override
    protected Tracer initialValue() {
      return NoOpTracer.INSTANCE;
    }
  };

  //-------------------------------------------------------------------------
  /**
   * Starts the process of tracing.
   * 
   * @param tracer  the tracer to use, not null
   */
  public static void start(Tracer tracer) {
    s_tracer.set(ArgumentChecker.notNull(tracer, "tracer"));
  }

  /**
   * Ends the process of tracing.
   * 
   * @return the call graph, null if not available
   */
  public static CallGraph end() {
    Tracer tracer = s_tracer.get();
    s_tracer.remove();
    CallGraphBuilder callGraphBuilder = tracer.getRoot();
    return callGraphBuilder == null ? null : callGraphBuilder.createTrace();
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private TracingProxy() {
  }

  //-------------------------------------------------------------------------
  @Override
  protected boolean decorate(Class<?> interfaceType, Class<?> implementationType) {
    return true;
  }

  @Override
  protected Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable {
    // this avoids recording calls to toString() in the debugger
    if (method.getName().equals("toString")) {
      return method.invoke(delegate, args);
    }
    Tracer tracer = s_tracer.get();
    tracer.called(method, args);
    try {
      Object retVal = method.invoke(delegate, args);
      tracer.returned(retVal);
      return retVal;
    } catch (Exception ex) {
      Throwable cause = EngineUtils.getCause(ex);
      tracer.threw(cause);
      throw cause;
    }
  }

}
