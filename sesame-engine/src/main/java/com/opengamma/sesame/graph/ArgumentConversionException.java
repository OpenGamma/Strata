/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.function.Parameter;

/**
 * Indicates an argument specified as a string couldn't be converted to the required type.
 */
class ArgumentConversionException extends InvalidGraphException {

  private final String _arg;

  /**
   * Creates an instance
   *
   * @param path the path of parameters to the problem, not null
   * @param message the descriptive message, not null
   */
  ArgumentConversionException(String arg, List<Parameter> path, String message, Exception cause) {
    super(path, message, cause);
    _arg = arg;
  }

  /**
   * @return the argument that couldn't be converted.
   */
  public String getArgument() {
    return _arg;
  }
}
