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
final class NoOpTracer extends Tracer {

  /**
   * Singleton implementation of the tracer.
   */
  static final Tracer INSTANCE = new NoOpTracer();

  /**
   * Restricted constructor.
   */
  private NoOpTracer() {
  }

  //-------------------------------------------------------------------------
  @Override
  void called(Method method, Object[] args) {
    // do nothing
  }

  @Override
  void returned(Object returnValue) {
    // do nothing
  }

  @Override
  void threw(Throwable ex) {
    // do nothing
  }

  @Override
  public CallGraph getRoot() {
    return null;
  }

}
