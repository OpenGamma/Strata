/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import com.opengamma.util.result.Result;

/**
 * Common interface for deliverable swap future calculators.
 */
public interface DeliverableSwapFutureCalculator {

  Result<Double> calculateSecurityModelPrice();

}
