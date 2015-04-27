/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.finance.fx.ExpandedFxSwap;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Calculates the present value parameter sensitivity of an {@code FxSwapTrade} for each of a set of scenarios.
 */
public class FxSwapPvParameterSensitivityFunction
    extends AbstractFxSwapFunction<CurveParameterSensitivity> {

  @Override
  protected CurveParameterSensitivity execute(ExpandedFxSwap product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity);
  }

}
