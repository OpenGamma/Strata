/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

/**
 * Exception thrown if the target of a link cannot be resolved.
 */
public class LinkResolutionException extends RuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception passing the failed link.
   *
   * @param message  the reason why the link could not be resolved, null tolerant
   */
  public LinkResolutionException(String message) {
    super(message);
  }

}
