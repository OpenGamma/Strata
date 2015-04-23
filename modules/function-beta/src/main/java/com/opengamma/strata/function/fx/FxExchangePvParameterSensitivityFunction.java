/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.finance.fx.FxExchange;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Calculates the present value of an {@code FxExchange} for each of a set of scenarios.
 */
public class FxExchangePvParameterSensitivityFunction
    extends AbstractFxExchangeFunction<CurveParameterSensitivity> {

  @Override
  protected CurveParameterSensitivity execute(FxExchange product, MarketDataRatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity);
  }

}
