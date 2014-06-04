/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.function.Parameter;

/**
 * Error node where there is no argument for a constructor and the parameter type is annotated to indicate
 * it's stored in the configuration database.
 */
public class MissingConfigNode extends ErrorNode {

  /**
   * Creates an instance.
   *
   * @param type the expected type of the object created by this node, not null
   * @param exception the exception that occurred, not null
   * @param parameter the parameter this node satisfies, null if it's the root node
   * TODO specific exception?
   */
  MissingConfigNode(Class<?> type, InvalidGraphException exception, Parameter parameter) {
    super(type, exception, parameter);
  }
}
