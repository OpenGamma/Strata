/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.google.common.collect.Multimap;

/**
 * Provides a hook to allow listeners to be added to all
 * calls made to components of a component map.
 */
// TODO - name of this interface could be improved as there is no direct connection with ComponentMap
public interface ProxiedComponentMap {

  /**
   * Called when a component has a method called. Returns the
   * result  object returned by the component.
   *
   * @param componentType the type of component called
   * @param result the result of the call
   */
  void receivedCall(Class<?> componentType, Object result);

  /**
   * Retrieve the set of components that were called and the data
   * that each of them returned.
   *
   * @return multimap of component -> data items
   */
  Multimap<Class<?>, Object> retrieveResults();
}
