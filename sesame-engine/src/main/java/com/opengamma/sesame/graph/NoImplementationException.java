/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * Exception used when no implementation can be found for a an interface.
 */
/* package */ class NoImplementationException extends InvalidGraphException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** The interface type for which no implementation could be found. */
  private final Class<?> _interfaceType;

  /**
   * Creates an instance
   *
   * @param interfaceType the interface type for which no implementation could be found.
   * @param path  the path of parameters to the problem, not null
   * @param message  the descriptive message, not null
   */
  /* package */ NoImplementationException(Class<?> interfaceType, List<Parameter> path, String message) {
    super(path, message);
    _interfaceType = ArgumentChecker.notNull(interfaceType, "interfaceType");
  }

  /**
   * @return the interface type for which no implementation could be found.
   */
  /* package */ Class<?> getInterfaceType() {
    return _interfaceType;
  }
}
