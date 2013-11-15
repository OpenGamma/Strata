/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;
import java.util.Objects;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;

/**
 * A node in the dependency model representing an existing object instance (e.g. a piece of infrastructure provided
 * by the engine or a singleton function).
 */
public final class ObjectNode extends Node {

  private final Class<?> _type;

  /* package */ ObjectNode(Class<?> type, Parameter parameter) {
    super(parameter);
    _type = type;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object create(ComponentMap componentMap, List<Object> dependencies) {
    return componentMap.getComponent(_type);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "component " + _type.getSimpleName();
  }

  public Class<?> getType() {
    return _type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ObjectNode other = (ObjectNode) obj;
    return Objects.equals(this._type, other._type);
  }
}
