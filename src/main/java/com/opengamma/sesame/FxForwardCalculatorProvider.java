/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.cache.Cache;

public interface FxForwardCalculatorProvider {

  @Cache
  FunctionResult<FxForwardCalculator> generateCalculator(FXForwardSecurity security);
}
