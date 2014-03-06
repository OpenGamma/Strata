/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.OpenGammaRuntimeException;

/**
 * Exception used in the graph build.
 */
/* package */ class GraphBuildException extends OpenGammaRuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance
   *
   * @param message the error message
   * @param exceptions the list of other exceptions, not null
   */
  /* package */ GraphBuildException(String message, List<InvalidGraphException> exceptions) {
    super(message);
    for (InvalidGraphException exception : exceptions) {
      addSuppressed(exception);
    }
  }
}
