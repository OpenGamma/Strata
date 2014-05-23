/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

/**
 * Listener for calls made on components.
 */
public interface ComponentListener {

  /**
   * Called whenever a call is made to a component. Note that
   * the request being made is not recorded, just the results.
   *
   * @param componentType the component being called
   * @param result the result of the call made
   */
  void receivedCall(Class<?> componentType, Object result);
}
