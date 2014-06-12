/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.function.Parameter;

/**
 * Error node indicating a type that the engine isn't able to construct.
 */
public class CannotBuildNode extends ErrorNode {

  /**
   * Creates an instance.
   *
   * @param type the expected type of the object created by this node, not null
   * @param exception the exception that occurred, not null
   * @param parameter the parameter this node satisfies, null if it's the root node
   */
  CannotBuildNode(Class<?> type, InvalidImplementationException exception, Parameter parameter) {
    super(type, exception, parameter);
  }

  /**
   * Creates an instance.
   *
   * @param type the expected type of the object created by this node, not null
   * @param exception the exception that occurred, not null
   * @param parameter the parameter this node satisfies, null if it's the root node
   */
  CannotBuildNode(Class<?> type, NoSuitableConstructorException exception, Parameter parameter) {
    super(type, exception, parameter);
  }
}
