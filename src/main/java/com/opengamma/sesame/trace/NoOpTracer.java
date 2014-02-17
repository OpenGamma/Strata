/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.lang.reflect.Method;

/**
 * Tracer implementation that does nothing.
 */
public final class NoOpTracer implements Tracer {

  /**
   * Singleton implementation of the tracer.
   */
  public static final Tracer INSTANCE = new NoOpTracer();

  /**
   * Restricted constructor.
   */
  private NoOpTracer() {
  }

  //-------------------------------------------------------------------------
  @Override
  public void called(Method method, Object[] args) {
    // do nothing
  }

  @Override
  public void returned(Object returnValue) {
    // do nothing
  }

  @Override
  public void threw(Throwable ex) {
    // do nothing
  }

  @Override
  public CallGraph getRoot() {
    return null;
  }

}
