/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.function.Parameter;

/**
 * A node in the function model defined as an interface.
 * <p>
 * This is used for an object that is referred to via an interface.
 * The implementation type is determined and created by the injection framework.
 */
public final class InterfaceNode extends ClassNode {

  /**
   * Creates an instance.
   * 
   * @param interfaceType  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   * @param implementationType  the implementation type to create, may be null
   * @param arguments  the list of nodes representing the arguments to the constructor, not null
   */
  InterfaceNode(Class<?> interfaceType, Class<?> implementationType, List<FunctionModelNode> arguments, Parameter parameter) {
    super(interfaceType, implementationType, arguments, parameter);
  }

  //-------------------------------------------------------------------------
  @Override
  protected String prettyPrintLine() {
    return getType().getSimpleName() + " (new " + getImplementationType().getSimpleName() + ")";
  }

}
