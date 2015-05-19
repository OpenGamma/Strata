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
 * Calculates the par rate sensitivity of a {@code SwapTrade} for each of a set of scenarios.
 */
public class SwapParRateSensitivityFunction
    extends AbstractSwapFunction<Double> {

  @Override
  protected Double execute(ExpandedSwap product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().parRateSensitivity(product, provider).build();
    return provider.parameterSensitivity(pointSensitivity).total();
  }

}
