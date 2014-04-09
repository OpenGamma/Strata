/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Function to retrieve SABR parameters from a source of data.
 */
public interface SABRInterestRateParametersFn {

  /**
   * Retrieve the SABR parameters from a source of data.
   *
   * @param env the environment in use
   * @param security the swaption  that SABR data is required for
   * @return a result containing the SABR parameters if successful, a
   * failure result otherwise
   */
  Result<SABRParametersConfig> getSabrParameters(Environment env, SwaptionSecurity security);
}
