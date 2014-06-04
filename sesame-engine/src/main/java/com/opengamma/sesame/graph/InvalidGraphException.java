/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;
import java.util.Objects;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * Base class for function model graph exceptions.
 */
public abstract class InvalidGraphException extends OpenGammaRuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** The path of parameters to the problem. */
  private final List<Parameter> _path;

  /** The error message. */
  private final String _message;

  /**
   * Creates an instance
   * 
   * @param path  the path of parameters to the problem, not null
   * @param message  the descriptive message, not null
   */
  InvalidGraphException(List<Parameter> path, String message) {
    super(message);
    _message = message;
    _path = ArgumentChecker.notNull(path, "path");
  }

  /**
   * Creates an instance
   *
   * @param path  the path of parameters to the problem, not null
   * @param message  the descriptive message, not null
   * @param cause  the underlying cause
   */
  InvalidGraphException(List<Parameter> path, String message, Throwable cause) {
    super(message, cause);
    _message = message;
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

  @Override
  public int hashCode() {
    return Objects.hash(_message);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final InvalidGraphException other = (InvalidGraphException) obj;
    return Objects.equals(this._message, other._message);
  }
}
