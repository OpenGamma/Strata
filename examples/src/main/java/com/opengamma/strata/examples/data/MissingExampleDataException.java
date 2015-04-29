/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.data;

/**
 * Runtime exception used by the example market data loader to indicate that required
 * example data is missing.
 */
public class MissingExampleDataException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an exception with the name of the missing example data resource.
   * 
   * @param resourceName  the resource name
   */
  public MissingExampleDataException(String resourceName) {
    super("Missing resource: " + resourceName);
  }

}
