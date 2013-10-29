/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class InvokerImpl implements Invoker {

  private final Object _target;

  public InvokerImpl(Object target) {
    ArgumentChecker.notNull(target, "target");
    _target = target;
  }

  @Override
  public Object invoke() {
    // TODO implement invoke()
    throw new UnsupportedOperationException("invoke not implemented");
  }
}
