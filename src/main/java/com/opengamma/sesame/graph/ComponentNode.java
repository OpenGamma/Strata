/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the dependency model representing an existing object instance (e.g. a piece of infrastructure provided
 * by the engine or a singleton function).
 */
public final class ComponentNode extends Node {

  /* package */ ComponentNode(Parameter parameter) {
    super(ArgumentChecker.notNull(parameter, "parameter").getType(), parameter);
  }

  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    return componentMap.getComponent(getType());
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "component " + getType().getSimpleName();
  }
}
