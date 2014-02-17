/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.id.ExternalScheme;

/**
 * Returns an {@link ExternalScheme} configured by a user argument.
 */
public class DefaultIdSchemeFn implements IdSchemeFn {

  /** The scheme to return from {@link #getScheme()} */
  private final ExternalScheme _scheme;

  /**
   * @param scheme The scheme to return from {@link #getScheme()}
   */
  public DefaultIdSchemeFn(ExternalScheme scheme) {
    if (scheme == null) {
      _scheme = ExternalSchemes.BLOOMBERG_TICKER;
    } else {
      _scheme = scheme;
    }
  }

  /**
   * @return An ID scheme
   */
  @Override
  public ExternalScheme getScheme() {
    return _scheme;
  }
}
