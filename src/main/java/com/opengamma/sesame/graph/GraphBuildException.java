/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

/**
 *
 */
/* package */ class GraphBuildException extends AbstractGraphBuildException {

  /* package */ GraphBuildException(String message, List<AbstractGraphBuildException> exceptions) {
    super(message);
    for (AbstractGraphBuildException exception : exceptions) {
      addSuppressed(exception);
    }
  }
}
