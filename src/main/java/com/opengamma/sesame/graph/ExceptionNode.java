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

/**
 *
 */
/* package */ class ExceptionNode extends Node {

  private final Exception _exception;

  /* package */ ExceptionNode(Class<?> type, Exception exception, Parameter parameter) {
    super(type, parameter);
    _exception = exception;
  }

  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    throw new OpenGammaRuntimeException("Can't build an invalid graph", _exception);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + _exception.toString();
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
