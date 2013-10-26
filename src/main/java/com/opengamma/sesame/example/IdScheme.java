/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.id.ExternalScheme;
import com.opengamma.sesame.function.UserParam;

/**
 * Returns an {@link ExternalScheme} configured by a user argument.
 */
public class IdScheme implements IdSchemeFunction {

  /** The scheme to return from {@link #getScheme()} */
  private final ExternalScheme _scheme;

  /**
   * @param scheme The scheme to return from {@link #getScheme()}
   */
  public IdScheme(@UserParam(name = "scheme", defaultValue = "BLOOMBERG_TICKER") ExternalScheme scheme) {
    _scheme = scheme;
  }

  /**
   * @return An ID scheme
   */
  @Override
  public ExternalScheme getScheme() {
    return _scheme;
  }
}
