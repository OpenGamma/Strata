/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

/**
 * A node in the dependency model representing an existing object instance (e.g. a piece of infrastructure provided
 * by the engine or a singleton function).
 */
public final class ObjectNode extends Node {

  private final Class<?> _type;

  /* package */ ObjectNode(Class<?> type) {
    _type = type;
  }

  @SuppressWarnings("unchecked")
  @Override
  Object create(Map<Class<?>, Object> singletons) {
    return singletons.get(_type);
  }

  public Class<?> getType() {
    return _type;
  }
}
