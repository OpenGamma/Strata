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
/* package */ final class NoOpTracer implements Tracer {

  /* package */ static final Tracer INSTANCE = new NoOpTracer();

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
  public Call getRoot() {
    throw new IllegalStateException();
  }
}
