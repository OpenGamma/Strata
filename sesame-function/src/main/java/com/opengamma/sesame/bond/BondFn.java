/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * General interface for bond.
 */
public interface BondFn {

  /**
   * Calculate the present value of a bond.
   *
   * @param env the environment that the PV will be calculate with.
   * @param bondTrade the bond trade to calculate the PV for.
   * @return result containing the present value if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(Environment env, BondTrade bondTrade);

}
