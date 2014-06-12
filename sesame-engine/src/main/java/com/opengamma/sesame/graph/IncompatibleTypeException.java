/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.function.Parameter;

/**
 * Indicates the type of an object injected into a constructor doesn't conform to the required type.
 */
public class IncompatibleTypeException extends InvalidGraphException {

  /**
   * Creates an instance
   *
   * @param path the path of parameters to the problem, not null
   * @param message the descriptive message, not null
   */
  /* package */ IncompatibleTypeException(List<Parameter> path, String message) {
    super(path, message);
  }
}
