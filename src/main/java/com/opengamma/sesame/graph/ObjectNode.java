/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

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
  public Object create(ComponentMap componentMap) {
    return componentMap.getComponent(_type);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "component " + _type.getSimpleName();
  }

  public Class<?> getType() {
    return _type;
  }
}
