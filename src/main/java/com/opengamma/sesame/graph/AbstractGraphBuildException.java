/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.List;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.function.Parameter;

/**
 *
 */
/* package */ abstract class AbstractGraphBuildException extends OpenGammaRuntimeException {

  private final List<Parameter> _path;

  /* package */ AbstractGraphBuildException(String message) {
    this(Collections.<Parameter>emptyList(), message);
  }

  /* package */ AbstractGraphBuildException(List<Parameter> path, String message) {
    super(message);
    _path = path;
  }

  public List<Parameter> getPath() {
    return _path;
  }
}
