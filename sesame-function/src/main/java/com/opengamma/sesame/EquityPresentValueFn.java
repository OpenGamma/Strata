/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;

// todo the Result<> bit is probably always there, would be nice if we could say OutputFunction<CashFlowSecurity, Double>

/**
 * Function capable of providing present value for equities.
 */
public interface EquityPresentValueFn {

  /**
   * Calculates present value for equities.
   * 
   *
   * @param env the execution environment
   * @param security  the equity, not null
   * @return the present value, a failure result if unable to calculate
   */
  @Output(OutputNames.PRESENT_VALUE)
  Result<Double> presentValue(Environment env, EquitySecurity security);

}
