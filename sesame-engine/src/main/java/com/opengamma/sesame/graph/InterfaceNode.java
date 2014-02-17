/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.function.Parameter;

/**
 * A node in the dependency model for an object referred to via an interface that must be created by the injection framework.
 */
public final class InterfaceNode extends ClassNode {

  public InterfaceNode(Class<?> interfaceType, Class<?> implementationType, List<Node> arguments, Parameter parameter) {
    super(interfaceType, implementationType, arguments, parameter);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + getType().getSimpleName() + " (new " + getImplementationType().getSimpleName() + ")";
  }
}
