/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.id.ExternalScheme;

/**
 * Returns an external ID scheme.
 */
public interface IdSchemeFn {

  /**
   * @return An external ID scheme.
   */
  ExternalScheme getScheme();
}
