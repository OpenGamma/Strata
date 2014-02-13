/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Calculate PV and a par rate for a FRA.
 */
public interface FRAPVFn {

  /**
   * Calculate the par rate for a FRA security.
   *
   * @param security the FRA to calculate the rate for
   * @return result containing the rate if successful, a Failure otherwise
   */
  @Output(value = OutputNames.PAR_RATE)
  Result<Double> calculateRate(FRASecurity security);

  /**
   * Calculate the present value for a FRA security.
   *
   * @param security the FRA to calculate the PV for
   * @return result containing the present value if successful, a Failure otherwise
   */
  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(FRASecurity security);
}
