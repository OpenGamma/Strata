/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.util.ArgumentChecker;

/**
 * Base implementation of a handler that stores the receiver.
 */
public abstract class AbstractProxyInvocationHandler implements ProxyInvocationHandler {

  /**
   * The receiver that this handler will call.
   */
  private final Object _receiver;

  /**
   * The actual (non-proxy) object that will eventually be called
   * by this handler. This will be the same as the {@link #_receiver}
   * when this is the first proxy wrapping an object.
   */
  private final Object _proxiedObject;

  /**
   * Creates an instance.
   * 
   * @param receiver  the receiver, not null
   */
  protected AbstractProxyInvocationHandler(Object receiver) {
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _proxiedObject = EngineUtils.getProxiedObject(receiver);
  }

  //-------------------------------------------------------------------------
  @Override
  public Object getReceiver() {
    return _receiver;
  }

  @Override
  public Object getProxiedObject() {
    return _proxiedObject;
  }

}
