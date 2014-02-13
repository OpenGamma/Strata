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
 * Calculate PV for a FRA.
 */
public interface FRAPVFn {

  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(FRASecurity security);
}
