/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.financial.analytics.model.fixedincome.SwapLegCashFlows;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculate analytics values for a Swap.
 */
public interface InterestRateSwapFn {

  /**
   * Calculate the par rate for a Swap security.
   *
   * @param env the environment used for calculation
   * @param security the Swap to calculate the rate for
   * @return result containing the rate if successful, a Failure otherwise
   */
  @Output(value = OutputNames.PAR_RATE)
  Result<Double> calculateParRate(Environment env, InterestRateSwapSecurity security);

  /**
   * Calculate the present value for a Swap security.
   *
   * @param env the environment used for calculation
   * @param security the Swap to calculate the PV for
   * @return result containing the present value if successful, a Failure otherwise
   */
  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateSwapSecurity security);

  /**
   * Calculate the PV01 for a Swap security.
   *
   * @param env the environment used for calculation
   * @param security the Swap to calculate the PV01 for
   * @return result containing the PV01 if successful, a Failure otherwise
   */
  @Output(value = OutputNames.PV01)
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateSwapSecurity security);

  /**
   * Calculate the receive leg cash flow for a Swap leg.
   *
   * @param env the environment used for calculation
   * @param security the InterestRateSwapSecurity to calculate the cash flows for
   * @return result containing the fixed cash flows if successful, a Failure otherwise
   */
  @Output(value = OutputNames.RECEIVE_LEG_CASH_FLOWS)
  Result<SwapLegCashFlows> calculateReceiveLegCashFlows(Environment env, InterestRateSwapSecurity security);

  /**
   * Calculate the pay leg cash flow for a Swap leg.
   *
   * @param env the environment used for calculation
   * @param security the InterestRateSwapSecurity to calculate the cash flows for
   * @return result containing the fixed cash flows if successful, a Failure otherwise
   */
  @Output(value = OutputNames.PAY_LEG_CASH_FLOWS)
  Result<SwapLegCashFlows> calculatePayLegCashFlows(Environment env, InterestRateSwapSecurity security);

  /**
   * Calculate the bucketed PV01 for a swap security.
   *
   * @param env the environment used for calculation
   * @param security the swap to calculate the bucketed PV01 for
   * @return result containing the bucketed PV01 if successful, a Failure otherwise
   */
  @Output(OutputNames.BUCKETED_PV01)
  Result<BucketedCurveSensitivities> calculateBucketedPV01(Environment env, InterestRateSwapSecurity security);

}
