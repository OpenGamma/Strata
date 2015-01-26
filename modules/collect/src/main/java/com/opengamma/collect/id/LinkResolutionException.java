/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.id;

import com.opengamma.collect.result.Failure;

/**
 * Exception thrown if the target of a link cannot be resolved.
 */
public class LinkResolutionException extends RuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception passing the failed link.
   *
   * @param link  the link which could not be resolved
   * @param causeMessage  the reason why the link could not be resolved
   */
  public LinkResolutionException(Link<?> link, String causeMessage) {
    super("Unable to resolve link: " + link + " - " + String.valueOf(causeMessage));
  }

  /**
   * Creates the exception passing the failed link.
   *
   * @param link  the link which could not be resolved
   * @param cause  the reason why the link could not be resolved
   */
  public LinkResolutionException(Link<?> link, Failure cause) {
    super("Unable to resolve link: " + link + " - " + (cause != null ? cause.getMessage() : "<no cause>"));
  }

}
