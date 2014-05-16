/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.sesame.cashflows.SwapLegCashFlows;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculator initialised with the data required to perform
 * analytics calculations for a particular security.
 */
public interface InterestRateSwapCalculator {

  /**
   * Calculates the present value for the security
   *
   * @return result containing the PV if successfully created, a failure result otherwise
   */
  Result<MultipleCurrencyAmount> calculatePV();

  /**
   * Calculates the par rate for the security
   *
   * @return result containing the rate if successfully created, a failure result otherwise
   */
  Result<Double> calculateRate();

  /**
   * Calculates the PV01 for the security
   *
   * @return result containing the PV01 if successfully created, a failure result otherwise
   */
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01();

  /**
   * Calculate the swap pay leg cash flow.
   *
   * @return result containing the cash flows if successful, a Failure otherwise
   */
  Result<SwapLegCashFlows> calculatePayLegCashFlows();

  /**
   * Calculate the swap receive leg cash flow.
   *
   * @return result containing the cash flows if successful, a Failure otherwise
   */
  Result<SwapLegCashFlows> calculateReceiveLegCashFlows();
}
