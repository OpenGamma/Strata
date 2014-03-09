/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

public interface FXForwardCalculatorFn {

  // it seems to be faster without caching on this method
  //@Cache
  Result<FXForwardCalculator> generateCalculator(Environment env, FXForwardSecurity security);
}
