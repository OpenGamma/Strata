/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.engine.ComponentMap;

/**
 *
 */
/* package */ class ExceptionNode extends Node {

  private final Exception _exception;

  /* package */ ExceptionNode(Exception exception) {
    _exception = exception;
  }

  @Override
  public Object create(ComponentMap componentMap) {
    throw new OpenGammaRuntimeException("Can't build an invalid graph", _exception);
  }
}
