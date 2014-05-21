/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

/**
 * Provides a hook to allow listeners to be added to all
 * calls made to components of a component map.
 */
// TODO - name of this interface could be improved as there is no direct connection with ComponentMap
public interface ProxiedComponentMap {

  /**
   * Add a listener to the proxy which will then be informed
   * of all requests made to components and the results
   * returned.
   *
   * @param listener the listener to be added
   */
  void addListener(ComponentListener listener);

  /**
   * Removes a listener from the proxy.
   *
   * @param listener the listener to be removed
   */
  void removeListener(ComponentListener listener);

  /**
   * Called when a component has a method called. Returns the
   * result  object returned by the component.
   *
   * @param componentType the type of component called
   * @param result the result of the call
   */
  void receivedCall(Class<?> componentType, Object result);
}
