/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

public interface InterestRateSwapCalculatorFn {

  Result<InterestRateSwapCalculator> generateCalculator(Environment env, InterestRateSwapSecurity security);

}
