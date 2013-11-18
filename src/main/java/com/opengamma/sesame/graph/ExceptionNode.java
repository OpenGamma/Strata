/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;

/**
 *
 */
/* package */ class ExceptionNode extends Node {

  private final Exception _exception;

  /* package */ ExceptionNode(Exception exception, Parameter parameter) {
    super(parameter);
    _exception = exception;
  }

  @Override
  public Object create(ComponentMap componentMap, List<Object> dependencies) {
    throw new OpenGammaRuntimeException("Can't build an invalid graph", _exception);
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + _exception.toString();
  }
}
