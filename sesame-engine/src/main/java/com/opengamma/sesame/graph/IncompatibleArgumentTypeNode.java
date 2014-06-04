/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.function.Parameter;

/**
 * Error node indicating the type of an argument isn't compatible with its parameter type.
 */
public class IncompatibleArgumentTypeNode extends ErrorNode {

  /**
   * Creates an instance.
   *
   * @param type the expected type of the object created by this node, not null
   * @param exception the exception that occurred, not null
   * @param parameter the parameter this node satisfies, null if it's the root node
   */
  IncompatibleArgumentTypeNode(Class<?> type, IncompatibleTypeException exception, Parameter parameter) {
    super(type, exception, parameter);
  }

  /**
   * @return the exception that triggered creation of the node
   */
  @Override
  public IncompatibleTypeException getException() {
    return (IncompatibleTypeException) super.getException();
  }
}
