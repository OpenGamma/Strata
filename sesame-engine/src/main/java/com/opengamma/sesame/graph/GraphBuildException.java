/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

/**
 * Exception used in the graph build.
 */
/* package */ class GraphBuildException extends AbstractGraphBuildException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance
   * 
   * @param path  the path of parameters to the problem, not null
   * @param exceptions  the list of other exceptions, not null
   */
  /* package */ GraphBuildException(String message, List<AbstractGraphBuildException> exceptions) {
    super(message);
    for (AbstractGraphBuildException exception : exceptions) {
      addSuppressed(exception);
    }
  }

}
