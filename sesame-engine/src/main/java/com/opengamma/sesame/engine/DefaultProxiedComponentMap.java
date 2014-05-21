/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * Proxies the components from a component map such that
 * registered listeners can be informed of any requests made.
 */
public class DefaultProxiedComponentMap implements ProxiedComponentMap {

  private final Set<ComponentListener> _listeners = new HashSet<>();

  @Override
  public void receivedCall(Class<?> componentType, Object result) {
    for (ComponentListener listener : _listeners) {
      listener.receivedCall(componentType, result);
    }
  }

  @Override
  public void addListener(ComponentListener listener) {
    _listeners.add(listener);
  }

  @Override
  public void removeListener(ComponentListener listener) {
    _listeners.remove(listener);
  }
}
