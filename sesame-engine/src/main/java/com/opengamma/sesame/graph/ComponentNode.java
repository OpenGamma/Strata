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
 * A node in the function model that supplies a component.
 * <p>
 * This represents an existing object instance, such as a piece of infrastructure
 * provided by the engine or a singleton function.
 */
public final class ComponentNode extends FunctionModelNode {

  /**
   * Creates an instance.
   * 
   * @param parameter  the parameter this node satisfies, null if it's the root node
   */
  ComponentNode(Parameter parameter) {
    super(ArgumentChecker.notNull(parameter, "parameter").getType(), parameter);
  }

  //-------------------------------------------------------------------------
  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    return componentMap.getComponent(getType());
  }

  @Override
  protected String prettyPrintLine() {
    return "component " + getType().getSimpleName();
  }

}
