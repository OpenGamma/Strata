/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.finance.fx.ExpandedFx;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the present value parameter sensitivity of an {@code FxForwardTrade} for each of a set of scenarios.
 */
public class FxForwardPvParameterSensitivityFunction
    extends AbstractFxForwardFunction<CurveParameterSensitivities> {

  @Override
  protected CurveParameterSensitivities execute(ExpandedFx product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity);
  }

}
