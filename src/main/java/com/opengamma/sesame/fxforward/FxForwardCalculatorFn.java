/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.FunctionResult;

public interface FxForwardCalculatorFn {

  // it seems to be faster without caching on this method
  //@Cache
  FunctionResult<FxForwardCalculator> generateCalculator(FXForwardSecurity security);
}
