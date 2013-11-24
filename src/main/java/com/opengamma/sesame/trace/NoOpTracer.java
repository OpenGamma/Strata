/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.lang.reflect.Method;

/**
 *
 */
public final class NoOpTracer implements Tracer {

  public static final Tracer INSTANCE = new NoOpTracer();

  private NoOpTracer() {
  }

  @Override
  public void called(Method method, Object[] args) {
    // do nothing
  }

  @Override
  public void returned(Object returnValue) {
    // do nothing
  }

  @Override
  public void threw(Throwable e) {
    // do nothing
  }

  @Override
  public CallGraph getRoot() {
    return null;
  }
}
