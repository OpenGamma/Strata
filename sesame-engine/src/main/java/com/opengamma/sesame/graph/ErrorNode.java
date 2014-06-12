/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the function model representing an error.
 */
public abstract class ErrorNode extends FunctionModelNode {

  /**
   * The error that occurred.
   */
  private final InvalidGraphException _exception;

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   * @param exception  the exception that occurred, not null
   */
  ErrorNode(Class<?> type, InvalidGraphException exception, Parameter parameter) {
    super(type, parameter);
    _exception = ArgumentChecker.notNull(exception, "exception");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the exception.
   * 
   * @return the exception, not null
   */
  public InvalidGraphException getException() {
    return _exception;
  }

  @Override
  public List<InvalidGraphException> getExceptions() {
    return Collections.singletonList(_exception);
  }

  //-------------------------------------------------------------------------
  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    throw new OpenGammaRuntimeException("Can't build an invalid graph", _exception);
  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  protected String prettyPrintLine() {
    return "ERROR: " + _exception.getMessage();
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ErrorNode other = (ErrorNode) obj;
    return Objects.equals(this._exception, other._exception);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_exception);
  }

}
