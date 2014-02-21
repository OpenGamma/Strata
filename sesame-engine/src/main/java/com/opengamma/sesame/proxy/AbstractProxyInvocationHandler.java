/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import com.opengamma.util.ArgumentChecker;

/**
 * Base implementation of a handler that stores the receiver.
 */
public abstract class AbstractProxyInvocationHandler implements ProxyInvocationHandler {

  /**
   * The receiver.
   */
  private final Object _receiver;

  /**
   * Creates an instance.
   * 
   * @param receiver  the receiver, not null
   */
  protected AbstractProxyInvocationHandler(Object receiver) {
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
  }

  //-------------------------------------------------------------------------
  @Override
  public Object getReceiver() {
    return _receiver;
  }

}
