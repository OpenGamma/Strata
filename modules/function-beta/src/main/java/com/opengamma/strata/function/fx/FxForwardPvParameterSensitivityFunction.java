/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.finance.fx.FxTransaction;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Calculates the present value of an {@code FxForward} for each of a set of scenarios.
 */
public class FxForwardPvParameterSensitivityFunction
    extends AbstractFxForwardFunction<CurveParameterSensitivity> {

  @Override
  protected CurveParameterSensitivity execute(FxTransaction product, MarketDataRatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity);
  }

}
