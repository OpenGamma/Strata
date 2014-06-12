/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.function.Parameter;

/**
 * Error node indicating that no value was provided for a non-nullable argument.
 */
public class MissingArgumentNode extends ErrorNode {

  /**
   * Creates an instance.
   *
   * @param type the expected type of the object created by this node, not null
   * @param exception the exception that occurred, not null
   * @param parameter the parameter this node satisfies, null if it's the root node
   */
  MissingArgumentNode(Class<?> type, MissingArgumentException exception, Parameter parameter) {
    super(type, exception, parameter);
  }
}
