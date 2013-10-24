/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.id.ExternalScheme;
import com.opengamma.sesame.config.DefaultImplementation;

/**
 * Returns an external ID scheme.
 */
@DefaultImplementation(IdScheme.class)
public interface IdSchemeFunction {

  /**
   * @return An external ID scheme.
   */
  ExternalScheme getScheme();
}
