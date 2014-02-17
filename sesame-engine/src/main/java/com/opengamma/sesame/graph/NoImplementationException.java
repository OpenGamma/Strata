/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.function.Parameter;

/**
 *
 */
/* package */ class NoImplementationException extends AbstractGraphBuildException {

  /* package */ NoImplementationException(List<Parameter> path, String message) {
    super(path, message);
  }
}
