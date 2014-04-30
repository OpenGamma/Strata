/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.sabr;

import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Provides a set of SABR parameters based on the
 * security passed.
 */
public interface SabrParametersProviderFn {

  /**
   * Retrieve the SABR parameters from a source of data.
   *
   * @param env  the environment in use, not null
   * @param security  the security that SABR data is required for, not null
   * @return a result containing the SABR parameters if successful,
   * a failure result otherwise, not null
   */
  Result<SabrParametersConfiguration> getSabrParameters(Environment env, FinancialSecurity security);
}
