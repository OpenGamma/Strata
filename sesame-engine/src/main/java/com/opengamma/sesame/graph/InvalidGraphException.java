/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * Base class for function model graph exceptions.
 */
/* package */ abstract class InvalidGraphException extends OpenGammaRuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The path of parameters to the problem.
   */
  private final List<Parameter> _path;

  /**
   * Creates an instance
   * 
   * @param path  the path of parameters to the problem, not null
   * @param message  the descriptive message, not null
   */
  InvalidGraphException(List<Parameter> path, String message) {
    super(message);
    _path = ArgumentChecker.notNull(path, "path");
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
