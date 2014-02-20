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
 * Base class for graph building exceptions.
 */
/* package */ abstract class AbstractGraphBuildException extends OpenGammaRuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The path of parameters to the problem.
   */
  private final List<Parameter> _path;

  /**
   * Creates an instance
   * 
   * @param message  the descriptive message, not null
   */
  /* package */ AbstractGraphBuildException(String message) {
    this(Collections.<Parameter>emptyList(), message);
  }

  /**
   * Creates an instance
   * 
   * @param path  the path of parameters to the problem, not null
   * @param message  the descriptive message, not null
   */
  /* package */ AbstractGraphBuildException(List<Parameter> path, String message) {
    super(message);
    _path = path;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the path of parameters to the problem.
   * 
   * @return the path, not null
   */
  public List<Parameter> getPath() {
    return _path;
  }

}
