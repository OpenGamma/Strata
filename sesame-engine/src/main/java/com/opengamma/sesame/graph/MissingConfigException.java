/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.function.Parameter;

/**
 * Exception used when there is no argument for a constructor and the parameter type is annotated to indicate
 * it's stored in the configuration database.
 */
public class MissingConfigException extends InvalidGraphException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance
   *
   * @param path  the path of parameters to the problem, not null
   * @param message  the descriptive message, not null
   */
  /* package */ MissingConfigException(List<Parameter> path, String message) {
    super(path, message);
  }

}
