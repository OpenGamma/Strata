/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

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

  /**
   * Calculate the bucketed PV01 of a bond.
   *
   *
   * @param env the environment that the bucketed PV01 will be calculate with.
   * @param bondTrade the bond trade to calculate the bucketed PV01 for.
   * @return result containing the present value if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.BUCKETED_PV01)
  Result<BucketedCurveSensitivities> calculateBucketedPV01(Environment env, BondTrade bondTrade);

  /**
   * Calculate the PV01 of a bond.
   *
   * @param env the environment that the PV01 will be calculate with.
   * @param bondTrade the bond trade to calculate the PV01 for.
   * @return result containing the present value if successful, a Failure otherwise.
   */
  @Output(value = OutputNames.PV01)
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, BondTrade bondTrade);

}
