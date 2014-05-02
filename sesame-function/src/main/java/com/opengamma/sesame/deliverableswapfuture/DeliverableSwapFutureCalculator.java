/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Common interface for deliverable swap future calculators.
 */
public interface DeliverableSwapFutureCalculator {

  /**
   * Calculates the price of the DeliverableSwapFuture via curves.
   *
   * @return result containing the Price if successfully created, a failure result otherwise.
   */
  Result<Double> calculateSecurityModelPrice();
  
  /**
   * Calculates the per curve PV01 of the DeliverableSwapFuture.
   *
   * @return result containing the, per curve, PV01 if successfully created, a failure result otherwise.
   */
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01();

}
