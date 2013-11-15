/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.security.fx.FXForwardSecurity;

public interface FxForwardCalculatorProvider {

  // it seems to be faster without caching on this method
  //@Cache
  FunctionResult<FxForwardCalculator> generateCalculator(FXForwardSecurity security);
}
