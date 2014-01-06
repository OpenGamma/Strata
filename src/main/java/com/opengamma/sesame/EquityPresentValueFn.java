/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;

// todo the Result<> bit is probably always there, would be nice if we could say OutputFunction<CashFlowSecurity, Double>

public interface EquityPresentValueFn {

  @Output(OutputNames.PRESENT_VALUE)
  Result<Double> presentValue(EquitySecurity security);
}
