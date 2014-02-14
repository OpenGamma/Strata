/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.util.result.Result;

public interface FRACalculatorFn {

  Result<FRACalculator> generateCalculator(FRASecurity security);
}
