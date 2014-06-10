/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.opengamma.id.UniqueIdentifiable;

/**
 * Proxies the components from a component map such that
 * registered listeners can be informed of any requests made.
 */
public class DefaultProxiedComponentMap implements ProxiedComponentMap {

  private final Multimap<Class<?>, UniqueIdentifiable> _componentDataRequests =
      Multimaps.synchronizedMultimap(HashMultimap.<Class<?>, UniqueIdentifiable>create());

  @Override
  public void receivedCall(Class<?> componentType, UniqueIdentifiable result) {
    _componentDataRequests.put(componentType, result);
  }

  @Override
  public Multimap<Class<?>, UniqueIdentifiable> retrieveResults() {
    // Strictly we should synchronize access as per Guava
    // documentation, but this method is called once all
    // entries have been added to the map
    return ImmutableMultimap.copyOf(_componentDataRequests);
  }
}
