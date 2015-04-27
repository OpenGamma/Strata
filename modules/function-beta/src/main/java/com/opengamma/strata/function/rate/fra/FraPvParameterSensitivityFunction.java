/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.fra;

import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Calculates the present value parameter sensitivity of a {@code FraTrade} for each of a set of scenarios.
 */
public class FraPvParameterSensitivityFunction
    extends AbstractFraFunction<CurveParameterSensitivity> {

  @Override
  protected CurveParameterSensitivity execute(ExpandedFra product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity);
  }

}
