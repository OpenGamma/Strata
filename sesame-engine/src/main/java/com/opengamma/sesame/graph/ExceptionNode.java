/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;
import java.util.Objects;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
/* package */ class ExceptionNode extends Node {

  private final AbstractGraphBuildException _exception;

  /* package */ ExceptionNode(Class<?> type, AbstractGraphBuildException exception, Parameter parameter) {
    super(type, parameter);
    _exception = ArgumentChecker.notNull(exception, "exception");
  }

  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    throw new OpenGammaRuntimeException("Can't build an invalid graph", _exception);
  }

  /**
   * @return false
   */
  @Override
  public boolean isValid() {
    return false;
  }

  /**
   * @return true
   */
  @Override
  public boolean isError() {
    return true;
  }

  /* package */ AbstractGraphBuildException getException() {
    return _exception;
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "ERROR: " + _exception.getMessage();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_exception);
  }

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
    final ExceptionNode other = (ExceptionNode) obj;
    return Objects.equals(this._exception, other._exception);
  }
}
