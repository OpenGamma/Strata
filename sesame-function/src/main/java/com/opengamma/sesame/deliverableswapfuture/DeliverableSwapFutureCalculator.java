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

  Result<Double> calculateSecurityModelPrice();
  
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01();

}
