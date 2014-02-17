/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.id.ExternalScheme;
import com.opengamma.util.result.Result;

/**
 * Returns an external ID scheme.
 */
public interface MockResultFn {

  Result<ExternalScheme> getScheme();

}
