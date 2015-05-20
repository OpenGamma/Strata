/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates PV01, the present value sensitivity of a {@code SwapTrade} for each of a set of scenarios.
 * This operates by algorithmic differentiation (AD).
 */
public class SwapPv01Function
    extends AbstractSwapFunction<Double> {

  @Override
  protected Double execute(ExpandedSwap product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider).build();
    return provider.parameterSensitivity(pointSensitivity).total();
  }

}
