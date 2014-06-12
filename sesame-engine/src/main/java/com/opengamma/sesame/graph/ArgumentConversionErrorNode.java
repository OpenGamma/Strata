/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * Error node indicating a string argument couldn't be converted to the expected type.
 */
public class ArgumentConversionErrorNode extends ErrorNode {

  private final String _value;
  private final String _errorMessage;

  /**
   * Creates an instance.
   *
   * @param type the expected type of the object created by this node, not null
   * @param exception the exception that occurred, not null
   * @param parameter the parameter this node satisfies, null if it's the root node
   */
  ArgumentConversionErrorNode(Class<?> type, InvalidGraphException exception, Parameter parameter, String value, String errorMessage) {
    super(type, exception, parameter);
    _value = ArgumentChecker.notEmpty(value, "value");
    _errorMessage = ArgumentChecker.notEmpty(errorMessage, "errorMessage");
  }

  /**
   * @return the argument value that couldn't be converted
   */
  public String getValue() {
    return _value;
  }

  /**
   * @return the error message
   */
  public String getErrorMessage() {
    return _errorMessage;
  }
}
