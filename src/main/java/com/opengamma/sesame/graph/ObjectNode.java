/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.engine.ComponentMap;

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
  public Object create(ComponentMap components) {
    return components.getComponent(_type);
  }

  public Class<?> getType() {
    return _type;
  }
}
