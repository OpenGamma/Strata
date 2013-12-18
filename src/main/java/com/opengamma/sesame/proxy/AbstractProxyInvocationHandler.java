/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public abstract class AbstractProxyInvocationHandler implements ProxyInvocationHandler {

  private final Object _receiver;

  protected AbstractProxyInvocationHandler(Object receiver) {
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
  }

  @Override
  public Object getReceiver() {
    return _receiver;
  }
}
