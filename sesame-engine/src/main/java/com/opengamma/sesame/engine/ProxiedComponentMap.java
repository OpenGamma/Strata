/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.google.common.collect.Multimap;
import com.opengamma.id.UniqueIdentifiable;

/**
 * Provides a hook to allow listeners to be added to all
 * calls made to components of a component map.
 */
// TODO - name of this interface could be improved as there is no direct connection with ComponentMap
public interface ProxiedComponentMap {

  /**
   * Called when a component has a method called. If a single method
   * call returns multiple items, this method will be called for
   * each individual item returned.
   *
   * @param componentType the type of component called
   * @param item the item returned from the component
   */
  void receivedCall(Class<?> componentType, UniqueIdentifiable item);

  /**
   * Retrieve the set of components that were called and the data
   * that each of them returned.
   *
   * @return multimap of component -> data items
   */
  Multimap<Class<?>, UniqueIdentifiable> retrieveResults();
}
